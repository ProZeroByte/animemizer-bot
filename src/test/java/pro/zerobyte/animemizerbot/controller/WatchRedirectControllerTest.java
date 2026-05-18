package pro.zerobyte.animemizerbot.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pro.zerobyte.animemizerbot.config.props.AnimeUrlProperties;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeLink;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.anime.AnimeSource;
import pro.zerobyte.animemizerbot.core.anime.AnimeWatchProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchRedirectControllerTest {

    private static final AnimeUrlProperties URL_PROPERTIES = new AnimeUrlProperties(
            Map.of(AnimeSource.ANILIBRIA, "https://anilibria.tv"),
            "https://animemizer.bot/watch?source=%s&publicId=%s"
    );

    @Mock
    private AnimeService animeService;

    @Mock
    private AnimeWatchProvider anilibriaProvider;

    private WatchRedirectController createController(List<AnimeWatchProvider> providers) {
        return new WatchRedirectController(animeService, URL_PROPERTIES, providers);
    }

    private Anime newAnimeWithLink(AnimeSource source, String externalId) {
        AnimeLink link = new AnimeLink(1L, 1L, source, externalId);
        return new Anime(1L, "shikimori_1", null, null, "Test Anime", "Тест", null,
                null, null, null, false, false, false, 12, null,
                null, null, null, null, Set.of(link));
    }

    private Anime newAnimeWithoutLinks() {
        return new Anime(1L, "shikimori_1", null, null, "Test Anime", "Тест", null,
                null, null, null, false, false, false, 12, null,
                null, null, null, null, Set.of());
    }

    @Nested
    @DisplayName("redirectToAnime")
    class RedirectToAnime {

        @Test
        void should_redirectToProviderUrl_when_animeAndLinkFound() {
            // Given
            when(anilibriaProvider.getSource()).thenReturn(AnimeSource.ANILIBRIA);
            WatchRedirectController controller = createController(List.of(anilibriaProvider));

            Anime anime = newAnimeWithLink(AnimeSource.ANILIBRIA, "anime/releases/release/42");
            when(animeService.findByPublicIdOrFetch("shikimori_1")).thenReturn(Optional.of(anime));

            // When
            ResponseEntity<Void> response = controller.redirectToAnime(AnimeSource.ANILIBRIA, "shikimori_1");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
            assertThat(response.getHeaders().getLocation()).isNotNull();
            assertThat(response.getHeaders().getLocation().toString()).isEqualTo("https://anilibria.tv/anime/releases/release/42");
        }

        @Test
        void should_return404_when_providerNotConfigured() {
            // Given
            when(anilibriaProvider.getSource()).thenReturn(AnimeSource.ANILIBRIA);
            WatchRedirectController controller = createController(List.of(anilibriaProvider));

            // When
            ResponseEntity<Void> response = controller.redirectToAnime(AnimeSource.YUMMYANIME, "shikimori_1");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void should_return404_when_animeNotFound() {
            // Given
            when(anilibriaProvider.getSource()).thenReturn(AnimeSource.ANILIBRIA);
            WatchRedirectController controller = createController(List.of(anilibriaProvider));

            when(animeService.findByPublicIdOrFetch("shikimori_999")).thenReturn(Optional.empty());

            // When
            ResponseEntity<Void> response = controller.redirectToAnime(AnimeSource.ANILIBRIA, "shikimori_999");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void should_return404_when_externalIdNotFound() {
            // Given
            when(anilibriaProvider.getSource()).thenReturn(AnimeSource.ANILIBRIA);
            when(anilibriaProvider.findExternalId(org.mockito.ArgumentMatchers.any(Anime.class)))
                    .thenReturn(Optional.empty());
            WatchRedirectController controller = createController(List.of(anilibriaProvider));

            Anime anime = newAnimeWithoutLinks();
            when(animeService.findByPublicIdOrFetch("shikimori_1")).thenReturn(Optional.of(anime));

            // When
            ResponseEntity<Void> response = controller.redirectToAnime(AnimeSource.ANILIBRIA, "shikimori_1");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
