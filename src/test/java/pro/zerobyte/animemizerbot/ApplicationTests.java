package pro.zerobyte.animemizerbot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pro.zerobyte.animemizerbot.config.TestTelegramConfig;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestTelegramConfig.class)
class ApplicationTests {

    private final AnimeRepository animeRepository;

    @Autowired
    ApplicationTests(AnimeRepository animeRepository) {
        this.animeRepository = animeRepository;
    }

    @Test
    void contextLoads() {
    }

    @Test
    void should_saveAndLoadAnime_when_animeLinkTableIsAbsent() {
        // Given
        Anime anime = new Anime(null, "shikimori_999999", null, null, "JDBC persistence test", null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                "https://example.com/watch");

        // When
        Anime savedAnime = animeRepository.save(anime);
        Anime loadedAnime = animeRepository.findById(savedAnime.id()).orElseThrow();

        // Then
        assertThat(loadedAnime.publicId()).isEqualTo(anime.publicId());
        assertThat(loadedAnime.watchUrl()).isEqualTo(anime.watchUrl());
    }
}
