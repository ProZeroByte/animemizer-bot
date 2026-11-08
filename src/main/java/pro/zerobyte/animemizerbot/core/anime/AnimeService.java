package pro.zerobyte.animemizerbot.core.anime;

import pro.zerobyte.animemizerbot.bot.message.TelegramMessageUtils;
import pro.zerobyte.animemizerbot.config.CacheConfig;
import pro.zerobyte.animemizerbot.integration.shikimori.ShikimoriConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
//TODO: Think about refactoring this service
public class AnimeService {
    private static final Logger log = LoggerFactory.getLogger(AnimeService.class);

    private final AnimeRepository repository;
    private final ShikimoriConnector shikimoriConnector;
    private final WatchLinkResolver watchLinkResolver;
    private final Cache providerFailuresCache;
    private final Cache animeByIdsCache;

    public AnimeService(AnimeRepository repository,
                        ShikimoriConnector shikimoriConnector,
                        WatchLinkResolver watchLinkResolver,
                        CacheManager cacheManager) {
        this.repository = repository;
        this.shikimoriConnector = shikimoriConnector;
        this.watchLinkResolver = watchLinkResolver;
        this.providerFailuresCache = cacheManager.getCache(CacheConfig.PROVIDER_FAILURES);
        this.animeByIdsCache = cacheManager.getCache(CacheConfig.ANIME_BY_ID);
    }

    public Anime save(Anime anime) {
        return repository.save(anime);
    }

    public Optional<Anime> findById(Long id) {
        return repository.findById(id).map(this::enrichAndSaveIfRequired);
    }

    public List<Anime> findAllById(List<Long> animeIds) {
        return repository.findAllById(animeIds);
    }

    public Optional<Anime> findByPublicIdOrFetch(String publicId) {
        return Optional.ofNullable(publicId)
                .filter(Predicate.not(String::isBlank))
                .flatMap(id -> repository.findByPublicId(id)
                        .or(() -> AnimeSource.SHIKIMORI.extractIdFromPublicId(id)
                                .flatMap(shikimoriConnector::findById)))
                .map(this::enrichAndSaveIfRequired); // Теперь ссылки докачиваются и здесь!
    }

    public Optional<Anime> findByTitle(String name) {
        return repository.findByRussianNameIgnoreCase(name)
                .or(() -> repository.findByEnglishNameIgnoreCase(name))
                .or(() -> repository.findByNameIgnoreCase(name))
                .or(() -> shikimoriConnector.findByTitle(name))
                .map(this::updateIfRequired);
    }

    //TODO: Think about refactoring this method
    private Anime updateIfRequired(Anime anime) {
        Optional<Long> key = AnimeSource.SHIKIMORI.extractIdFromPublicId(anime.publicId());
        boolean isCachedByShikimoriId = key.isPresent() && animeByIdsCache.get(key.get(), Anime.class) != null;
        if (anime.id() !=null && anime.isOngoing() && key.isPresent() && !isCachedByShikimoriId) {
            anime = shikimoriConnector.findById(key.get()).orElse(anime);
        }
        return enrichAndSaveIfRequired(anime);
    }

    /**
     * Enriches anime with a watch link and saves to DB.
     *
     * <p><b>Side effect:</b> This method mutates and persists the entity even when called from
     * read-only contexts like {@code findById} or {@code findByPublicIdOrFetch}. This is an
     * intentional design choice — watch link enrichment and description cleanup happen
     * lazily on first access. Consider extracting enrichment into a dedicated
     * {@code EnrichmentService} if callers need a read-only path.
     */
    //TODO: Think about refactoring this method
    private Anime enrichAndSaveIfRequired(Anime anime) {
        boolean isNewEntity = anime.id() == null;
        boolean needsWatchUrl = anime.watchUrl() == null || anime.watchUrl().isBlank();

        // Если аниме уже есть в БД и ссылка на месте - ничего не делаем
        if (!needsWatchUrl && !isNewEntity) {
            return anime;
        }

        Anime toSave = anime;

        if (needsWatchUrl) {
            String cacheKey = buildFailureCacheKey(anime.id(), AnimeSource.SHIKIMORI);

            if (cacheKey != null && isCachedFailure(cacheKey)) {
                log.debug("Skipping watch link resolution for anime '{}' — cached failure", anime.russianName());
            } else {
                log.info("Resolving watch link for anime '{}'...", anime.russianName());
                Optional<String> watchUrl = watchLinkResolver.resolveWatchUrl(anime);
                if (watchUrl.isPresent()) {
                    toSave = toSave.withWatchUrl(watchUrl.get());
                } else {
                    cacheFailure(cacheKey);
                }
            }
        }

        // Защита от race condition: если аниме новое, но уже существует в БД по publicId —
        // подставляем существующий id, чтобы выполнить UPDATE вместо INSERT
        if (isNewEntity && anime.publicId() != null) {
            Optional<Anime> existing = repository.findByPublicId(anime.publicId());
            if (existing.isPresent()) {
                toSave = toSave.withId(existing.get().id());
            }
        }

        // Очистка описания через regex-пайплайн
        if (toSave.description() != null && !toSave.description().isBlank()) {
            String cleanedDesc = TelegramMessageUtils.cleanDescription(toSave.description());
            if (!cleanedDesc.equals(toSave.description())) {
                log.debug("Description cleaned for anime {}: {} → {} chars",
                        toSave.russianName(), toSave.description().length(), cleanedDesc.length());
                toSave = toSave.withDescription(cleanedDesc);
            }
        }

        return this.save(toSave);
    }

    private String buildFailureCacheKey(Long animeId, AnimeSource source) {
        if (animeId == null || source == null) {
            return null;
        }
        return animeId + ":" + source.name();
    }

    private boolean isCachedFailure(String cacheKey) {
        if (providerFailuresCache == null) {
            return false;
        }
        return providerFailuresCache.get(cacheKey, Boolean.class) != null;
    }

    private void cacheFailure(String cacheKey) {
        if (providerFailuresCache == null || cacheKey == null) {
            return;
        }
        providerFailuresCache.put(cacheKey, Boolean.TRUE);
        log.debug("Cached provider failure for key '{}'", cacheKey);
    }
}
