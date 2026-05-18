package pro.zerobyte.animemizerbot.core.anime;

import pro.zerobyte.animemizerbot.bot.message.TelegramMessageUtils;
import pro.zerobyte.animemizerbot.integration.shikimori.ShikimoriConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@Service
//TODO: Think about refactoring this service
public class AnimeService {
    private static final Logger log = LoggerFactory.getLogger(AnimeService.class);
    private static final String FAILURE_CACHE_NAME = "providerCheckFailures";

    private final AnimeRepository repository;
    private final ShikimoriConnector shikimoriConnector;
    private final List<AnimeWatchProvider> activeProviders;
    private final Cache providerCheckFailuresCache;

    public AnimeService(AnimeRepository repository,
                        ShikimoriConnector shikimoriConnector,
                        List<AnimeWatchProvider> activeProviders,
                        CacheManager cacheManager) {
        this.repository = repository;
        this.shikimoriConnector = shikimoriConnector;
        this.activeProviders = activeProviders;
        this.providerCheckFailuresCache = cacheManager.getCache(FAILURE_CACHE_NAME);
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
                .map(this::enrichAndSaveIfRequired);
    }

    /**
     * Enriches anime with external provider links and saves to DB.
     *
     * <p><b>Side effect:</b> This method mutates and persists the entity even when called from
     * read-only contexts like {@code findById} or {@code findByPublicIdOrFetch}. This is an
     * intentional design choice — provider link enrichment and description cleanup happen
     * lazily on first access. Consider extracting enrichment into a dedicated
     * {@code EnrichmentService} if callers need a read-only path.
     */
    private Anime enrichAndSaveIfRequired(Anime anime) {
        List<AnimeWatchProvider> missingProviders = activeProviders.stream()
                .filter(provider -> !anime.hasLinkForSource(provider.getSource()))
                .toList();

        boolean isNewEntity = anime.id() == null;

        // Если аниме уже есть в БД и все ссылки на месте - ничего не делаем
        if (missingProviders.isEmpty() && !isNewEntity) {
            return anime;
        }

        // Безопасное копирование коллекции (защита от NPE)
        Set<AnimeLink> updatedLinks = new HashSet<>(anime.links() != null ? anime.links() : Set.of());

        if (!missingProviders.isEmpty()) {
            log.info("Found missing providers {} for anime {}. Fetching...", missingProviders, anime.russianName());
            missingProviders.forEach(provider -> {
                String cacheKey = buildFailureCacheKey(anime.id(), provider.getSource());

                if (cacheKey != null && isCachedFailure(cacheKey)) {
                    log.debug("Skipping provider {} for anime '{}' — cached failure", provider.getSource(), anime.russianName());
                    return;
                }

                provider.findExternalId(anime)
                        .ifPresentOrElse(
                                parsedId -> updatedLinks.add(new AnimeLink(null, anime.id(), provider.getSource(), parsedId)),
                                () -> cacheFailure(cacheKey));
            });
        }

        Anime toSave = anime.withLinks(Set.copyOf(updatedLinks));

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
        if (providerCheckFailuresCache == null) {
            return false;
        }
        return providerCheckFailuresCache.get(cacheKey, Boolean.class) != null;
    }

    private void cacheFailure(String cacheKey) {
        if (providerCheckFailuresCache == null || cacheKey == null) {
            return;
        }
        providerCheckFailuresCache.put(cacheKey, Boolean.TRUE);
        log.debug("Cached provider failure for key '{}'", cacheKey);
    }
}
