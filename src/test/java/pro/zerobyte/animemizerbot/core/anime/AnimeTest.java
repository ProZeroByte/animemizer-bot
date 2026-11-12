package pro.zerobyte.animemizerbot.core.anime;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AnimeTest {

    private Anime newAnime(Long id, String publicId, String name) {
        return new Anime(id, publicId, null, null, name, "Russian", null,
                "desc", "poster.jpg", "tv", true, false, false, 12, 12,
                LocalDate.now(), LocalDate.now(), LocalDateTime.now(), null, "https://example.com/watch");
    }

    @Test
    void should_preserveWatchUrl_when_idChanges() {
        // Given
        Anime anime = newAnime(100L, "shikimori_100", "Test Anime");

        // When
        Anime result = anime.withId(200L);

        // Then
        assertThat(result.id()).isEqualTo(200L);
        assertThat(result.watchUrl()).isEqualTo(anime.watchUrl());
    }

    @Test
    void should_preserveWatchUrl_when_descriptionChanges() {
        // Given
        Anime anime = newAnime(100L, "shikimori_100", "Test Anime");

        // When
        Anime result = anime.withDescription("Updated description");

        // Then
        assertThat(result.description()).isEqualTo("Updated description");
        assertThat(result.watchUrl()).isEqualTo(anime.watchUrl());
    }

    @Test
    void should_replaceWatchUrl_when_newWatchUrlProvided() {
        // Given
        Anime anime = newAnime(100L, "shikimori_100", "Test Anime");

        // When
        Anime result = anime.withWatchUrl("https://example.com/new-watch");

        // Then
        assertThat(result.watchUrl()).isEqualTo("https://example.com/new-watch");
    }
}
