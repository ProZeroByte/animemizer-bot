package pro.zerobyte.animemizerbot.integration.kinopoisk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import pro.zerobyte.animemizerbot.config.props.AnilibertyProperties;
import pro.zerobyte.animemizerbot.config.props.OpenRouterProperties;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.integration.aniliberty.AnilibertyClient;
import pro.zerobyte.animemizerbot.integration.aniliberty.dto.AnilibertyReleaseDto;
import pro.zerobyte.animemizerbot.integration.kinopoisk.dto.KinopoiskSearchResponse;
import pro.zerobyte.animemizerbot.integration.openrouter.OpenRouterClient;
import pro.zerobyte.animemizerbot.integration.openrouter.dto.ChatCompletionRequest;
import pro.zerobyte.animemizerbot.integration.openrouter.dto.ChatCompletionResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KinopoiskWatchLinkResolverTest {

    @Mock
    private KinopoiskClient kinopoiskClient;

    @Mock
    private AnilibertyClient anilibertyClient;

    @Mock
    private OpenRouterClient openRouterClient;

    private KinopoiskWatchLinkResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new KinopoiskWatchLinkResolver(
                kinopoiskClient,
                anilibertyClient,
                openRouterClient,
                new OpenRouterProperties("test-key", null, "test-model"),
                new AnilibertyProperties(null));
    }

    private Anime newAnime(String name, String russianName, String publicId) {
        return new Anime(1L, publicId, null, null, name, russianName, null,
                null, null, null, false, false, false, 12, null,
                null, null, null, null, null);
    }

    private KinopoiskSearchResponse kinopoiskResponse(KinopoiskSearchResponse.KinopoiskFilm... films) {
        return new KinopoiskSearchResponse("keyword", 1, films.length, List.of(films));
    }

    private KinopoiskSearchResponse.KinopoiskFilm kinopoiskFilm(long id, String nameRu, String type, String year) {
        return new KinopoiskSearchResponse.KinopoiskFilm(
                id, nameRu, null, type, year, null, null, null, null, null, null);
    }

    private AnilibertyReleaseDto anilibertyRelease(long id, String alias, String title, Long shikimoriId) {
        AnilibertyReleaseDto.AnilibertyNameDto name =
                new AnilibertyReleaseDto.AnilibertyNameDto(title, null, null);
        AnilibertyReleaseDto.AnilibertyShikimoriDto shikimori = shikimoriId == null
                ? null
                : new AnilibertyReleaseDto.AnilibertyShikimoriDto(shikimoriId, null, null, null);
        return new AnilibertyReleaseDto(id, null, 2007, name, alias, shikimori, 450, false);
    }

    private ChatCompletionResponse selection(String content) {
        return new ChatCompletionResponse(List.of(
                new ChatCompletionResponse.Choice(new ChatCompletionResponse.Message(content))));
    }

    @Nested
    @DisplayName("resolveWatchUrl")
    class ResolveWatchUrl {

        @Test
        void should_rewriteSelectedKinopoiskSeriesUrlToKinokinoRed_when_matchFound() {
            // Given
            Anime anime = newAnime("Naruto: Shippuuden", "Наруто Ураганные хроники", "shikimori_1735");
            when(kinopoiskClient.searchByKeyword("Наруто Ураганные хроники"))
                    .thenReturn(kinopoiskResponse(
                            kinopoiskFilm(301, "Наруто", "FILM", "2002"),
                            kinopoiskFilm(283290, "Наруто: Ураганные хроники", "TV_SERIES", "2007")));
            when(openRouterClient.complete(any(ChatCompletionRequest.class))).thenReturn(selection("283290"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://www.kinokino.red/series/283290/");
        }

        @Test
        void should_useOpenRouterToSelectKinopoiskCandidate_when_searchReturnsMultipleFilms() {
            // Given
            Anime anime = newAnime("Naruto", "Наруто", "shikimori_20");
            when(kinopoiskClient.searchByKeyword("Наруто"))
                    .thenReturn(kinopoiskResponse(
                            kinopoiskFilm(301, "Наруто", "FILM", "2002"),
                            kinopoiskFilm(283290, "Наруто: Ураганные хроники", "TV_SERIES", "2007")));
            when(openRouterClient.complete(any(ChatCompletionRequest.class))).thenReturn(selection("301"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://www.kinokino.red/film/301/");
            ArgumentCaptor<ChatCompletionRequest> captor = ArgumentCaptor.forClass(ChatCompletionRequest.class);
            verify(openRouterClient).complete(captor.capture());
            assertThat(captor.getValue().model()).isEqualTo("test-model");
            assertThat(captor.getValue().messages().getLast().content())
                    .contains("Наруто", "filmId: 301", "filmId: 283290");
        }

        @Test
        void should_useCommonSeries_when_laterSeasonSearchReturnsBaseTitleAndOpenRouterFails() {
            // Given
            Anime anime = newAnime("Sasaki and Peeps 2", "Сасаки и Пи 2", "shikimori_58518");
            when(kinopoiskClient.searchByKeyword("Сасаки и Пи 2"))
                    .thenReturn(kinopoiskResponse(kinopoiskFilm(5632822, "Сасаки и Пи", "TV_SERIES", "2024")));
            when(openRouterClient.complete(any(ChatCompletionRequest.class))).thenReturn(null);

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://www.kinokino.red/series/5632822/");
        }

        @Test
        void should_searchBaseTitle_when_laterSeasonSearchHasNoCandidates() {
            // Given
            Anime anime = newAnime("Sasaki and Peeps 2", "Сасаки и Пи 2 сезон", "shikimori_58518");
            when(kinopoiskClient.searchByKeyword("Сасаки и Пи 2 сезон")).thenReturn(kinopoiskResponse());
            when(kinopoiskClient.searchByKeyword("Сасаки и Пи"))
                    .thenReturn(kinopoiskResponse(kinopoiskFilm(5632822, "Сасаки и Пи 1 сезон", "TV_SERIES", "2024")));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://www.kinokino.red/series/5632822/");
        }

        @Test
        void should_preferSelectedSeason_when_commonSeriesIsAlsoAvailable() {
            // Given
            Anime anime = newAnime("Sasaki 2", "Сасаки и Пи 2", "shikimori_58518");
            when(kinopoiskClient.searchByKeyword("Сасаки и Пи 2"))
                    .thenReturn(kinopoiskResponse(kinopoiskFilm(1, "Сасаки и Пи", "TV_SERIES", "2024"),
                            kinopoiskFilm(2, "Сасаки и Пи 2", "TV_SERIES", "2026")));
            when(openRouterClient.complete(any(ChatCompletionRequest.class))).thenReturn(selection("2"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://www.kinokino.red/series/2/");
        }

        @Test
        void should_notUseUnrelatedOrAmbiguousTitles_when_searchingCommonSeries() {
            // Given
            Anime anime = newAnime("Sasaki 2", "Сасаки и Пи 2", "shikimori_58518");
            when(kinopoiskClient.searchByKeyword("Сасаки и Пи 2")).thenReturn(kinopoiskResponse());
            when(kinopoiskClient.searchByKeyword("Сасаки и Пи"))
                    .thenReturn(kinopoiskResponse(kinopoiskFilm(1, "Сасаки и Пи", "TV_SERIES", "2024"),
                            kinopoiskFilm(2, "Сасаки и Пи 1 сезон", "TV_SERIES", "2025"),
                            kinopoiskFilm(3, "Сасаки и Пи: Фильм", "FILM", "2024")));
            when(anilibertyClient.searchReleases("Сасаки и Пи 2")).thenReturn(List.of());

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_searchBaseTitle_when_englishSeasonSuffixIsUsed() {
            // Given
            Anime anime = newAnime("Example Season 2", null, "shikimori_123");
            when(kinopoiskClient.searchByKeyword("Example Season 2")).thenReturn(kinopoiskResponse());
            when(kinopoiskClient.searchByKeyword("Example"))
                    .thenReturn(kinopoiskResponse(kinopoiskFilm(12, "Example", "TV_SERIES", "2024")));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://www.kinokino.red/series/12/");
        }

        @Test
        void should_ignoreOtherSeasonsAndFilms_when_searchingCommonSeries() {
            // Given
            Anime anime = newAnime("Example 2", null, "shikimori_123");
            when(kinopoiskClient.searchByKeyword("Example 2")).thenReturn(kinopoiskResponse());
            when(kinopoiskClient.searchByKeyword("Example"))
                    .thenReturn(kinopoiskResponse(kinopoiskFilm(1, "Example 3", "TV_SERIES", "2026"),
                            kinopoiskFilm(2, "Example", "FILM", "2024"),
                            kinopoiskFilm(3, "Example Extra", "TV_SERIES", "2024")));
            when(anilibertyClient.searchReleases("Example 2")).thenReturn(List.of());

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_fallBackToAniliberty_when_KinopoiskHasNoCandidates() {
            // Given
            Anime anime = newAnime("Naruto: Shippuuden", "Наруто Ураганные хроники", "shikimori_1735");
            when(kinopoiskClient.searchByKeyword("Наруто Ураганные хроники"))
                    .thenReturn(kinopoiskResponse());
            when(anilibertyClient.searchReleases("Наруто Ураганные хроники"))
                    .thenReturn(List.of(anilibertyRelease(413, "naruto-shippuuden-naruto-uragannye-khroniki",
                            "Наруто Ураганные хроники", 1735L)));
            when(openRouterClient.complete(any(ChatCompletionRequest.class))).thenReturn(selection("999"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://www.aniliberty.top/anime/releases/release/"
                    + "naruto-shippuuden-naruto-uragannye-khroniki/episodes");
            ArgumentCaptor<ChatCompletionRequest> captor = ArgumentCaptor.forClass(ChatCompletionRequest.class);
            verify(openRouterClient).complete(captor.capture());
            assertThat(captor.getValue().messages().getLast().content())
                    .contains("Shikimori id: 1735", "shikimori.id: 1735");
        }

        @Test
        void should_returnEmpty_when_noTitleAvailable() {
            // Given
            Anime anime = newAnime(null, null, "shikimori_1735");

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_fallBackToAniliberty_when_KinopoiskSearchFails() {
            // Given
            Anime anime = newAnime("Naruto", "Наруто", "shikimori_1735");
            when(kinopoiskClient.searchByKeyword("Наруто"))
                    .thenThrow(new RestClientException("Kinopoisk unavailable"));
            when(anilibertyClient.searchReleases("Наруто"))
                    .thenReturn(List.of(anilibertyRelease(413, "naruto", "Наруто", 1735L)));
            when(openRouterClient.complete(any(ChatCompletionRequest.class))).thenReturn(selection("413"));

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).contains("https://www.aniliberty.top/anime/releases/release/naruto/episodes");
        }

        @Test
        void should_returnEmpty_when_OpenRouterDeclinesCandidates() {
            // Given
            Anime anime = newAnime("Naruto", "Наруто", "shikimori_1735");
            when(kinopoiskClient.searchByKeyword("Наруто"))
                    .thenReturn(kinopoiskResponse(kinopoiskFilm(301, "Наруто", "FILM", "2002")));
            when(openRouterClient.complete(any(ChatCompletionRequest.class))).thenReturn(selection("NONE"));
            when(anilibertyClient.searchReleases("Наруто")).thenReturn(List.of());

            // When
            Optional<String> result = resolver.resolveWatchUrl(anime);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
