package pro.zerobyte.animemizerbot.core.anime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AnimeTest {

    private Anime newAnime(Long id, String publicId, String name, Set<AnimeLink> links) {
        return new Anime(id, publicId, null, null, name, "Russian", null,
                "desc", "poster.jpg", "tv", true, false, false, 12, 12,
                LocalDate.now(), LocalDate.now(), LocalDateTime.now(), null, links);
    }

    @Nested
    @DisplayName("hasLinkForSource")
    class HasLinkForSource {

        @Test
        void should_returnTrue_when_linkExistsForSource() {
            // Given
            AnimeLink link = new AnimeLink(1L, 100L, AnimeSource.ANILIBRIA, "ext-1");
            Anime anime = newAnime(100L, "shikimori_100", "Test Anime", Set.of(link));

            // When
            boolean result = anime.hasLinkForSource(AnimeSource.ANILIBRIA);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnFalse_when_noLinks() {
            // Given
            Anime anime = newAnime(100L, "shikimori_100", "Test Anime", Set.of());

            // When
            boolean result = anime.hasLinkForSource(AnimeSource.ANILIBRIA);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void should_returnFalse_when_nullLinks() {
            // Given
            Anime anime = newAnime(100L, "shikimori_100", "Test Anime", null);

            // When
            boolean result = anime.hasLinkForSource(AnimeSource.ANILIBRIA);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void should_returnFalse_when_differentSourceOnly() {
            // Given
            AnimeLink link = new AnimeLink(1L, 100L, AnimeSource.ANIMEGO, "ext-1");
            Anime anime = newAnime(100L, "shikimori_100", "Test Anime", Set.of(link));

            // When
            boolean result = anime.hasLinkForSource(AnimeSource.ANILIBRIA);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("withId")
    class WithId {

        @Test
        void should_returnNewAnimeWithDifferentId() {
            // Given
            AnimeLink link = new AnimeLink(1L, 100L, AnimeSource.ANILIBRIA, "ext-1");
            Anime anime = newAnime(100L, "shikimori_100", "Test Anime", Set.of(link));

            // When
            Anime result = anime.withId(200L);

            // Then
            assertThat(result.id()).isEqualTo(200L);
        }

        @Test
        void should_preserveAllOtherFields() {
            // Given
            AnimeLink link = new AnimeLink(1L, 100L, AnimeSource.ANILIBRIA, "ext-1");
            Anime anime = newAnime(100L, "shikimori_100", "Test Anime", Set.of(link));

            // When
            Anime result = anime.withId(200L);

            // Then
            assertThat(result.publicId()).isEqualTo(anime.publicId());
            assertThat(result.name()).isEqualTo(anime.name());
            assertThat(result.russianName()).isEqualTo(anime.russianName());
            assertThat(result.description()).isEqualTo(anime.description());
            assertThat(result.posterUrl()).isEqualTo(anime.posterUrl());
            assertThat(result.kind()).isEqualTo(anime.kind());
            assertThat(result.isReleased()).isEqualTo(anime.isReleased());
            assertThat(result.episodes()).isEqualTo(anime.episodes());
            assertThat(result.episodesAired()).isEqualTo(anime.episodesAired());
            assertThat(result.airedOn()).isEqualTo(anime.airedOn());
            assertThat(result.releasedOn()).isEqualTo(anime.releasedOn());
            assertThat(result.updatedAt()).isEqualTo(anime.updatedAt());
            assertThat(result.links()).isEqualTo(anime.links());
        }
    }

    @Nested
    @DisplayName("withDescription")
    class WithDescription {

        @Test
        void should_returnNewAnimeWithNewDescription() {
            // Given
            Anime anime = newAnime(100L, "shikimori_100", "Test Anime", Set.of());

            // When
            Anime result = anime.withDescription("Updated description");

            // Then
            assertThat(result.description()).isEqualTo("Updated description");
        }

        @Test
        void should_preserveAllOtherFields() {
            // Given
            AnimeLink link = new AnimeLink(1L, 100L, AnimeSource.ANILIBRIA, "ext-1");
            Anime anime = newAnime(100L, "shikimori_100", "Test Anime", Set.of(link));

            // When
            Anime result = anime.withDescription("Updated description");

            // Then
            assertThat(result.id()).isEqualTo(anime.id());
            assertThat(result.publicId()).isEqualTo(anime.publicId());
            assertThat(result.name()).isEqualTo(anime.name());
            assertThat(result.russianName()).isEqualTo(anime.russianName());
            assertThat(result.posterUrl()).isEqualTo(anime.posterUrl());
            assertThat(result.kind()).isEqualTo(anime.kind());
            assertThat(result.isReleased()).isEqualTo(anime.isReleased());
            assertThat(result.episodes()).isEqualTo(anime.episodes());
            assertThat(result.episodesAired()).isEqualTo(anime.episodesAired());
            assertThat(result.airedOn()).isEqualTo(anime.airedOn());
            assertThat(result.releasedOn()).isEqualTo(anime.releasedOn());
            assertThat(result.updatedAt()).isEqualTo(anime.updatedAt());
            assertThat(result.links()).isEqualTo(anime.links());
        }
    }

    @Nested
    @DisplayName("withLinks")
    class WithLinks {

        @Test
        void should_returnNewAnimeWithNewLinks() {
            // Given
            Anime anime = newAnime(100L, "shikimori_100", "Test Anime", Set.of());
            AnimeLink newLink = new AnimeLink(2L, 100L, AnimeSource.ANIMEGO, "ext-2");

            // When
            Anime result = anime.withLinks(Set.of(newLink));

            // Then
            assertThat(result.links()).containsExactly(newLink);
        }

        @Test
        void should_preserveAllOtherFields() {
            // Given
            AnimeLink link = new AnimeLink(1L, 100L, AnimeSource.ANILIBRIA, "ext-1");
            Anime anime = newAnime(100L, "shikimori_100", "Test Anime", Set.of(link));
            AnimeLink newLink = new AnimeLink(2L, 100L, AnimeSource.ANIMEGO, "ext-2");

            // When
            Anime result = anime.withLinks(Set.of(newLink));

            // Then
            assertThat(result.id()).isEqualTo(anime.id());
            assertThat(result.publicId()).isEqualTo(anime.publicId());
            assertThat(result.name()).isEqualTo(anime.name());
            assertThat(result.russianName()).isEqualTo(anime.russianName());
            assertThat(result.description()).isEqualTo(anime.description());
            assertThat(result.posterUrl()).isEqualTo(anime.posterUrl());
            assertThat(result.kind()).isEqualTo(anime.kind());
            assertThat(result.isReleased()).isEqualTo(anime.isReleased());
            assertThat(result.episodes()).isEqualTo(anime.episodes());
            assertThat(result.episodesAired()).isEqualTo(anime.episodesAired());
            assertThat(result.airedOn()).isEqualTo(anime.airedOn());
            assertThat(result.releasedOn()).isEqualTo(anime.releasedOn());
            assertThat(result.updatedAt()).isEqualTo(anime.updatedAt());
        }
    }
}
