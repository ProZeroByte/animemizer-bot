package pro.zerobyte.animemizerbot.integration.tavily;

import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.WatchLinkResolver;
import pro.zerobyte.animemizerbot.integration.tavily.dto.TavilySearchRequest;
import pro.zerobyte.animemizerbot.integration.tavily.dto.TavilySearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

/**
 * Resolves a single watch URL for an anime using Tavily web search.
 *
 * <p>Resolution order:
 * <ol>
 *     <li>Direct kinopoisk.ru link, rewritten to kinokino.win</li>
 *     <li>aniliberty.top link</li>
 *     <li>General web search with the Russian title</li>
 * </ol>
 */
@Service
public class TavilyWatchLinkResolver implements WatchLinkResolver {

    private static final Logger log = LoggerFactory.getLogger(TavilyWatchLinkResolver.class);

    private static final String KINOPOISK_DOMAIN = "kinopoisk.ru";
    private static final String KINOKINO_DOMAIN = "kinokino.win";
    private static final String ANILIBERTY_DOMAIN = "aniliberty.top";
    private static final String SEARCH_DEPTH = "basic";
    private static final int MAX_RESULTS = 5;

    private final TavilyClient tavilyClient;

    public TavilyWatchLinkResolver(TavilyClient tavilyClient) {
        this.tavilyClient = tavilyClient;
    }

    @Override
    public Optional<String> resolveWatchUrl(Anime anime) {
        String title = resolveTitle(anime);
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        Optional<String> kinopoisk = searchFirstUrl(title, List.of(KINOPOISK_DOMAIN))
                .map(url -> url.replace(KINOPOISK_DOMAIN, KINOKINO_DOMAIN));
        if (kinopoisk.isPresent()) {
            log.debug("Resolved kinokino link for anime '{}': {}", title, kinopoisk.get());
            return kinopoisk;
        }

        Optional<String> aniliberty = searchFirstUrl(title, List.of(ANILIBERTY_DOMAIN));
        if (aniliberty.isPresent()) {
            log.debug("Resolved aniliberty link for anime '{}': {}", title, aniliberty.get());
            return aniliberty;
        }

        Optional<String> general = searchFirstUrl(title + " смотреть онлайн русский перевод", List.of());
        general.ifPresentOrElse(
                url -> log.debug("Resolved general link for anime '{}': {}", title, url),
                () -> log.debug("No watch link found for anime '{}'", title));
        return general;
    }

    private Optional<String> searchFirstUrl(String query, List<String> includeDomains) {
        try {
            TavilySearchRequest request = new TavilySearchRequest(query, SEARCH_DEPTH, MAX_RESULTS, includeDomains);
            TavilySearchResponse response = tavilyClient.search(request);
            if (response == null || response.results() == null) {
                return Optional.empty();
            }
            return response.results().stream()
                    .map(TavilySearchResponse.TavilySearchResult::url)
                    .filter(url -> url != null && !url.isBlank())
                    .findFirst();
        } catch (RestClientException e) {
            log.warn("Tavily search failed for query '{}': {}", query, e.getMessage());
            return Optional.empty();
        }
    }

    private String resolveTitle(Anime anime) {
        if (anime.russianName() != null && !anime.russianName().isBlank()) {
            return anime.russianName();
        }
        return anime.name();
    }
}
