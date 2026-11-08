package pro.zerobyte.animemizerbot.integration.tavily;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.integration.tavily.dto.TavilySearchRequest;
import pro.zerobyte.animemizerbot.integration.tavily.dto.TavilySearchResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TavilyWatchLinkResolverTest {

    @Mock
    private TavilyClient tavilyClient;

    @InjectMocks
    private TavilyWatchLinkResolver resolver;

    private Anime newAnime(String name, String russianName) {
        return new Anime(1L, "shikimori_1", null, null, name, russianName, null,
                null, null, null, false, false, false, 12, null, null, null, null, null, null, null);
    }

    private TavilySearchResponse responseWith(String... urls) {
        List<TavilySearchResponse.TavilySearchResult> results = java.util.Arrays.stream(urls)
                .map(url -> new TavilySearchResponse.TavilySearchResult("title", url, "content", 0.9))
                .toList();
        return new TavilySearchResponse("query", results);
    }

    @Nested
    @DisplayName("resolveWatchUrl")
    class ResolveWatchUrl {

        @Test
        void should_rewriteKinopoiskToKinokino_when_kinopoiskFound() {
            // Given
            Anime anime = newAnime("Steins;Gate", "Врата Штейна");
            when(tavilyClient.search(any(TavilySearchRequest.class)))
                    .thenReturn(responseWith("https://www.kinopoisk.ru/series/123456/"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://www.kinokino.win/series/123456/");
        }

        @Test
        void should_fallBackToAniliberty_when_kinopoiskNotFound() {
            // Given
            Anime anime = newAnime("Steins;Gate", "Врата Штейна");
            when(tavilyClient.search(any(TavilySearchRequest.class)))
                    .thenReturn(responseWith())
                    .thenReturn(responseWith("https://www.aniliberty.top/anime/releases/release/42"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://www.aniliberty.top/anime/releases/release/42");
        }

        @Test
        void should_fallBackToGeneralSearch_when_kinopoiskAndAnilibertyNotFound() {
            // Given
            Anime anime = newAnime("Steins;Gate", "Врата Штейна");
            when(tavilyClient.search(any(TavilySearchRequest.class)))
                    .thenReturn(responseWith())
                    .thenReturn(responseWith())
                    .thenReturn(responseWith("https://example.com/watch/steins-gate"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://example.com/watch/steins-gate");
        }

        @Test
        void should_useRussianTitleInGeneralQuery_when_russianNamePresent() {
            // Given
            Anime anime = newAnime("Steins;Gate", "Врата Штейна");
            when(tavilyClient.search(any(TavilySearchRequest.class)))
                    .thenReturn(responseWith())
                    .thenReturn(responseWith())
                    .thenReturn(responseWith("https://example.com/watch"));

            // When
            resolver.resolveWatchUrl(anime);

            // Then
            ArgumentCaptor<TavilySearchRequest> captor = ArgumentCaptor.forClass(TavilySearchRequest.class);
            verify(tavilyClient, org.mockito.Mockito.times(3)).search(captor.capture());
            assertThat(captor.getAllValues().get(2).query()).contains("Врата Штейна");
        }

        @Test
        void should_returnEmpty_when_noResults() {
            // Given
            Anime anime = newAnime("Steins;Gate", "Врата Штейна");
            when(tavilyClient.search(any(TavilySearchRequest.class))).thenReturn(responseWith());

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_titleBlank() {
            // Given
            Anime anime = newAnime(null, null);

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_clientThrows() {
            // Given
            Anime anime = newAnime("Steins;Gate", "Врата Штейна");
            when(tavilyClient.search(any(TavilySearchRequest.class)))
                    .thenThrow(new RestClientException("boom"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
