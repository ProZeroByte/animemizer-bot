package pro.zerobyte.animemizerbot.integration.tavily;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import pro.zerobyte.animemizerbot.config.props.OpenRouterProperties;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.integration.openrouter.OpenRouterClient;
import pro.zerobyte.animemizerbot.integration.openrouter.dto.ChatCompletionRequest;
import pro.zerobyte.animemizerbot.integration.openrouter.dto.ChatCompletionResponse;
import pro.zerobyte.animemizerbot.integration.tavily.dto.TavilySearchRequest;
import pro.zerobyte.animemizerbot.integration.tavily.dto.TavilySearchResponse;

import java.util.Arrays;
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

    @Mock
    private OpenRouterClient openRouterClient;

    private TavilyWatchLinkResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TavilyWatchLinkResolver(tavilyClient, openRouterClient, openRouterProperties());
    }

    private OpenRouterProperties openRouterProperties() {
        return new OpenRouterProperties("test-key", null, null);
    }

    private Anime newAnime(String name, String russianName) {
        return new Anime(1L, "shikimori_1", null, null, name, russianName, null,
                null, null, null, false, false, false, 12, null, null, null, null, null, null, null);
    }

    private TavilySearchResponse responseWith(String... urls) {
        List<TavilySearchResponse.TavilySearchResult> results = Arrays.stream(urls)
                .map(url -> new TavilySearchResponse.TavilySearchResult("title", url, "content", 0.9))
                .toList();
        return new TavilySearchResponse("query", results);
    }

    private TavilySearchResponse responseWithResults(TavilySearchResponse.TavilySearchResult... results) {
        return new TavilySearchResponse("query", List.of(results));
    }

    private ChatCompletionResponse selection(String content) {
        return new ChatCompletionResponse(List.of(
                new ChatCompletionResponse.Choice(new ChatCompletionResponse.Message(content))));
    }

    @Nested
    @DisplayName("resolveWatchUrl")
    class ResolveWatchUrl {

        @Test
        void should_rewriteSelectedKinopoiskUrl_when_kinopoiskResultsExist() {
            // Given
            Anime anime = newAnime("Steins;Gate", "Врата Штейна");
            when(tavilyClient.search(any(TavilySearchRequest.class)))
                    .thenReturn(responseWith("https://www.kinopoisk.ru/series/123456/",
                            "https://kinopoisk.ru/series/789/"));
            when(openRouterClient.complete(any(ChatCompletionRequest.class)))
                    .thenReturn(selection("https://kinopoisk.ru/series/789/"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://kinokino.win/series/789/");
        }

        @Test
        void should_ignoreKinopoiskNonTitlePages_when_onlyMediaArticlesReturned() {
            // Given
            Anime anime = newAnime("Steins;Gate", "Врата Штейна");
            when(tavilyClient.search(any(TavilySearchRequest.class)))
                    .thenReturn(responseWith("https://www.kinopoisk.ru/media/article/4010659",
                            "https://www.kinopoisk.ru/lists/m_act[keyword]/728434"))
                    .thenReturn(responseWith())
                    .thenReturn(responseWith("https://example.com/watch/steins-gate"));
            when(openRouterClient.complete(any(ChatCompletionRequest.class)))
                    .thenReturn(selection("https://example.com/watch/steins-gate"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://example.com/watch/steins-gate");
            ArgumentCaptor<ChatCompletionRequest> captor = ArgumentCaptor.forClass(ChatCompletionRequest.class);
            verify(openRouterClient).complete(captor.capture());
            assertThat(captor.getValue().messages().getLast().content())
                    .doesNotContain("kinopoisk.ru/media", "kinopoisk.ru/lists");
        }

        @Test
        void should_useProviderPrompt_when_selectingKinopoiskTitlePage() {
            // Given
            Anime anime = newAnime("Darker than Black", "Темнее чёрного");
            when(tavilyClient.search(any(TavilySearchRequest.class)))
                    .thenReturn(responseWith("https://www.kinopoisk.ru/series/647199",
                            "https://www.kinopoisk.ru/series/408714/reviews"));
            when(openRouterClient.complete(any(ChatCompletionRequest.class)))
                    .thenReturn(selection("https://www.kinopoisk.ru/series/647199"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://www.kinokino.win/series/647199");
            ArgumentCaptor<ChatCompletionRequest> captor = ArgumentCaptor.forClass(ChatCompletionRequest.class);
            verify(openRouterClient).complete(captor.capture());
            String systemPrompt = captor.getValue().messages().getFirst().content();
            assertThat(systemPrompt).contains("main title page");
            assertThat(systemPrompt).doesNotContain("Russian voice-over");
            assertThat(captor.getValue().messages().getLast().content())
                    .doesNotContain("kinopoisk.ru/series/408714/reviews");
        }

        @Test
        void should_fallBackToAniliberty_when_kinopoiskHasNoCandidates() {
            // Given
            Anime anime = newAnime("Steins;Gate", "Врата Штейна");
            when(tavilyClient.search(any(TavilySearchRequest.class)))
                    .thenReturn(responseWith())
                    .thenReturn(responseWith("https://www.aniliberty.top/anime/releases/release/42"));
            when(openRouterClient.complete(any(ChatCompletionRequest.class)))
                    .thenReturn(selection("https://www.aniliberty.top/anime/releases/release/42"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://www.aniliberty.top/anime/releases/release/42");
        }

        @Test
        void should_useGeneralRussianDubSearch_when_targetedProvidersHaveNoCandidates() {
            // Given
            Anime anime = newAnime("Steins;Gate", "Врата Штейна");
            when(tavilyClient.search(any(TavilySearchRequest.class)))
                    .thenReturn(responseWith())
                    .thenReturn(responseWith())
                    .thenReturn(responseWith("https://example.com/watch/steins-gate"));
            when(openRouterClient.complete(any(ChatCompletionRequest.class)))
                    .thenReturn(selection("https://example.com/watch/steins-gate"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://example.com/watch/steins-gate");
            ArgumentCaptor<TavilySearchRequest> captor = ArgumentCaptor.forClass(TavilySearchRequest.class);
            verify(tavilyClient, org.mockito.Mockito.times(3)).search(captor.capture());
            TavilySearchRequest generalRequest = captor.getAllValues().getLast();
            assertThat(generalRequest.query()).contains("русская озвучка", "русский дубляж");
            assertThat(generalRequest.country()).isEqualTo("russia");
            assertThat(generalRequest.language()).isEqualTo("ru");
            assertThat(generalRequest.includeDomainsMode()).isEqualTo("prefer");
            assertThat(generalRequest.includeDomains()).contains("yummyanime.tv", "aniliberty.top");
        }

        @Test
        void should_fallBackToGeneralTavilyResults_when_openRouterFails() {
            // Given
            Anime anime = newAnime("Steins;Gate", "Врата Штейна");
            when(tavilyClient.search(any(TavilySearchRequest.class)))
                    .thenReturn(responseWith("https://kinopoisk.ru/series/123/"))
                    .thenReturn(responseWith())
                    .thenReturn(responseWithResults(
                            new TavilySearchResponse.TavilySearchResult("Other anime", "https://example.com/other", "Original audio", 0.9),
                            new TavilySearchResponse.TavilySearchResult("Врата Штейна — русская озвучка", "https://example.com/ru-dub", "Смотреть онлайн", 0.8)));
            when(openRouterClient.complete(any(ChatCompletionRequest.class)))
                    .thenThrow(new RestClientException("OpenRouter unavailable"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://example.com/ru-dub");
        }

        @Test
        void should_rejectUrlNotReturnedByTavily_when_modelInventsUrl() {
            // Given
            Anime anime = newAnime("Steins;Gate", "Врата Штейна");
            when(tavilyClient.search(any(TavilySearchRequest.class)))
                    .thenReturn(responseWith())
                    .thenReturn(responseWith())
                    .thenReturn(responseWithResults(new TavilySearchResponse.TavilySearchResult(
                            "Врата Штейна — русская озвучка", "https://example.com/tavily-result", "Смотреть", 0.8)));
            when(openRouterClient.complete(any(ChatCompletionRequest.class)))
                    .thenReturn(selection("https://invented.example/watch"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://example.com/tavily-result");
        }

        @Test
        void should_returnEmpty_when_noRussianDubResultFound() {
            // Given
            Anime anime = newAnime("Steins;Gate", "Врата Штейна");
            when(tavilyClient.search(any(TavilySearchRequest.class)))
                    .thenReturn(responseWith())
                    .thenReturn(responseWith())
                    .thenReturn(responseWithResults(new TavilySearchResponse.TavilySearchResult(
                            "Врата Штейна — русские субтитры", "https://example.com/not-confirmed", "Смотреть онлайн", 0.9)));
            when(openRouterClient.complete(any(ChatCompletionRequest.class)))
                    .thenReturn(selection("NONE"));

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
        void should_includeRussianTitleAndCandidatesInOpenRouterRequest() {
            // Given
            Anime anime = newAnime("Steins;Gate", "Врата Штейна");
            when(tavilyClient.search(any(TavilySearchRequest.class)))
                    .thenReturn(responseWith("https://www.kinopoisk.ru/series/123456/"));
            when(openRouterClient.complete(any(ChatCompletionRequest.class)))
                    .thenReturn(selection("https://www.kinopoisk.ru/series/123456/"));

            // When
            resolver.resolveWatchUrl(anime);

            // Then
            ArgumentCaptor<ChatCompletionRequest> captor = ArgumentCaptor.forClass(ChatCompletionRequest.class);
            verify(openRouterClient).complete(captor.capture());
            assertThat(captor.getValue().model()).isEqualTo("openai/gpt-4o-mini");
            assertThat(captor.getValue().messages().getLast().content())
                    .contains("Врата Штейна", "https://www.kinopoisk.ru/series/123456/");
        }
    }
}
