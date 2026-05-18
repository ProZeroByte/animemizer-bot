package pro.zerobyte.animemizerbot.integration.shikimori;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import pro.zerobyte.animemizerbot.config.CacheConfig;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.integration.shikimori.dto.ShikimoriAnimeDto;
import pro.zerobyte.animemizerbot.integration.shikimori.dto.ShikimoriAnimeRelatedDto;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Component
public class ShikimoriConnector {
    private static final Logger log = LoggerFactory.getLogger(ShikimoriConnector.class);

    private final ShikimoriClient shikimoriClient;
    private final ShikimoriMapper shikimoriMapper;
    private final TaskExecutor virtualThreadExecutor;

    public ShikimoriConnector(ShikimoriClient shikimoriClient,
                              ShikimoriMapper shikimoriMapper,
                              @Qualifier("applicationTaskExecutor") TaskExecutor virtualThreadExecutor) {
        this.shikimoriClient = shikimoriClient;
        this.shikimoriMapper = shikimoriMapper;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    @Cacheable(value = CacheConfig.ANIME_BY_ID, key = "#id", unless = "#result == null or T(java.util.Optional).empty().equals(#result)")
    public Optional<Anime> findById(Long id) {
        try {
            CompletableFuture<ShikimoriAnimeDto> detailsFuture = fetchDetailsAsync(id);
            CompletableFuture<List<ShikimoriAnimeRelatedDto>> relatedFuture = fetchRelatedAsync(id);
            Anime anime = detailsFuture.thenCombine(relatedFuture, shikimoriMapper::toEntity).join();
            return Optional.of(anime);
        } catch (CompletionException e) {
            log.error("Failed to fetch anime data for ID {}: {}", id, e.getMessage());
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            log.error("HTTP error during anime fetch for ID {}: {} {}", id, e.getStatusCode(), e.getMessage());
            return Optional.empty();
        }
    }

    @Cacheable(value = CacheConfig.ANIME_BY_TITLE, key = "#title.toLowerCase()", unless = "#result == null or T(java.util.Optional).empty().equals(#result)")
    public Optional<Anime> findByTitle(String title) {
        try {
            List<ShikimoriAnimeDto> searchResults = shikimoriClient.searchAnime(title, 1);
            if (searchResults == null || searchResults.isEmpty()) {
                return Optional.empty();
            }
            return findById(searchResults.getFirst().id());
        } catch (CompletionException e) {
            log.error("Failed to search anime by title '{}': {}", title, e.getMessage());
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            log.error("HTTP error during anime search for title '{}': {} {}", title, e.getStatusCode(), e.getMessage());
            return Optional.empty();
        }
    }

    private CompletableFuture<ShikimoriAnimeDto> fetchDetailsAsync(Long animeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return shikimoriClient.getAnimeDetails(animeId);
            } catch (HttpClientErrorException e) {
                throw new CompletionException(e);
            }
        }, virtualThreadExecutor);
    }

    private CompletableFuture<List<ShikimoriAnimeRelatedDto>> fetchRelatedAsync(Long animeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return shikimoriClient.getRelatedAnimes(animeId);
            } catch (HttpClientErrorException e) {
                throw new CompletionException(e);
            }
        }, virtualThreadExecutor);
    }
}
