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
    private final Cache animeByTitleCache;
    private final Cache animeRefreshCooldownCache;

    public AnimeService(AnimeRepository repository,
                        ShikimoriConnector shikimoriConnector,
                        WatchLinkResolver watchLinkResolver,
                        CacheManager cacheManager) {
        this.repository = repository;
        this.shikimoriConnector = shikimoriConnector;
        this.watchLinkResolver = watchLinkResolver;
        this.providerFailuresCache = cacheManager.getCache(CacheConfig.PROVIDER_FAILURES);
        this.animeByIdsCache = cacheManager.getCache(CacheConfig.ANIME_BY_ID);
        this.animeByTitleCache = cacheManager.getCache(CacheConfig.ANIME_BY_TITLE);
        this.animeRefreshCooldownCache = cacheManager.getCache(CacheConfig.ANIME_REFRESH_COOLDOWN);
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

    /**
     * Force-syncs an anime with fresh Shikimori data, bypassing both the database copy
     * and the {@code animeById} cache. Used by the refresh button for ongoing anime:
     * newly released episodes are not visible through regular lookups because they read
     * from the database or a cached Shikimori response.
     *
     * <p>Rate-limited by the {@code animeRefreshCooldown} cache (12h TTL): at most two
     * live fetches per anime per day. While the cooldown is active the database copy is
     * returned without touching Shikimori. The cooldown slot is claimed before the fetch
     * so concurrent taps cannot bypass it, and released again when Shikimori fails so a
     * retry is possible.
     *
     * <p>When the record is missing, the public ID is not a Shikimori one, or Shikimori
     * is unreachable, returns the unchanged database copy (or {@link Optional#empty()}
     * when there is no record at all).
     */
    public Optional<Anime> refreshById(Long animeId) {
        Optional<Anime> existing = repository.findById(animeId);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Anime current = existing.get();
        Optional<Long> shikimoriId = AnimeSource.SHIKIMORI.extractIdFromPublicId(current.publicId());
        if (shikimoriId.isEmpty()) {
            log.warn("Cannot refresh anime {}: publicId '{}' is not a Shikimori id", animeId, current.publicId());
            return Optional.of(current);
        }

        if (isRefreshOnCooldown(animeId)) {
            log.debug("Skipping refresh for anime {}: cooldown active", animeId);
            return Optional.of(current);
        }

        // Claim the cooldown slot before fetching so concurrent taps cannot bypass the limit
        claimRefreshCooldown(animeId);

        // Bypass the 24h @Cacheable window so the connector performs a live Shikimori fetch
        animeByIdsCache.evict(shikimoriId.get());

        Optional<Anime> fresh = shikimoriConnector.findById(shikimoriId.get());
        if (fresh.isEmpty()) {
            releaseRefreshCooldown(animeId);
            log.warn("Shikimori fetch failed for anime {} (shikimori id {}); keeping database copy",
                    animeId, shikimoriId.get());
            return Optional.of(current);
        }

        Anime merged = mergeFresh(current, fresh.get());
        Anime saved = repository.save(merged);

        evictTitleCache(current);
        evictTitleCache(saved);

        log.info("Refreshed anime '{}' (id {}): episodes {} -> {}, ongoing {}",
                saved.russianName(), animeId, current.episodes(), saved.episodes(), saved.isOngoing());
        return Optional.of(saved);
    }

    /**
     * Returns true when the anime was successfully refreshed from Shikimori within the
     * cooldown window (12h). Used by the refresh callback handler to do nothing while
     * the limit is active.
     */
    public boolean isRefreshOnCooldown(Long animeId) {
        if (animeRefreshCooldownCache == null || animeId == null) {
            return false;
        }
        return animeRefreshCooldownCache.get(animeId, Boolean.class) != null;
    }

    private void claimRefreshCooldown(Long animeId) {
        if (animeRefreshCooldownCache == null || animeId == null) {
            return;
        }
        animeRefreshCooldownCache.put(animeId, Boolean.TRUE);
    }

    private void releaseRefreshCooldown(Long animeId) {
        if (animeRefreshCooldownCache == null || animeId == null) {
            return;
        }
        animeRefreshCooldownCache.evict(animeId);
    }

    /**
     * Merges fresh Shikimori data into an existing database entity. Fresh values win for
     * all Shikimori-sourced fields; the database {@code id} and the already resolved
     * {@code watchUrl} are preserved (the mapper leaves both null).
     */
    private Anime mergeFresh(Anime current, Anime fresh) {
        String cleanedDescription = fresh.description();
        if (cleanedDescription != null && !cleanedDescription.isBlank()) {
            cleanedDescription = TelegramMessageUtils.cleanDescription(cleanedDescription);
        }
        return new Anime(
                current.id(),
                fresh.publicId(),
                fresh.prequelPublicId(),
                fresh.sequelPublicId(),
                fresh.name(),
                fresh.russianName(),
                fresh.englishName(),
                cleanedDescription,
                fresh.posterUrl(),
                fresh.kind(),
                fresh.isReleased(),
                fresh.isAnons(),
                fresh.isOngoing(),
                fresh.episodes(),
                fresh.episodesAired(),
                fresh.airedOn(),
                fresh.releasedOn(),
                fresh.updatedAt(),
                fresh.nextEpisodeAt(),
                current.watchUrl()
        );
    }

    private void evictTitleCache(Anime anime) {
        evictTitleCacheKey(anime.name());
        evictTitleCacheKey(anime.russianName());
        evictTitleCacheKey(anime.englishName());
    }

    private void evictTitleCacheKey(String title) {
        if (title == null || title.isBlank() || animeByTitleCache == null) {
            return;
        }
        animeByTitleCache.evict(title.toLowerCase());
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
