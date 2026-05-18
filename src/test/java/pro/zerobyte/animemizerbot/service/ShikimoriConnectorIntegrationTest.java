package pro.zerobyte.animemizerbot.service;

import pro.zerobyte.animemizerbot.config.TestTelegramConfig;
import pro.zerobyte.animemizerbot.integration.shikimori.ShikimoriConnector;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestTelegramConfig.class)
class ShikimoriConnectorIntegrationTest {

    @Autowired
    private ShikimoriConnector shikimoriConnector;

    @Nested
    @DisplayName("findByTitle")
    class FindByTitle {

        @Test
        void should_returnAnime_when_titleExistsOnShikimori() {
            // Given
            String title = "Naruto";

            // When
            Optional<Anime> result = shikimoriConnector.findByTitle(title);

            // Then
            assertThat(result).isPresent();
            Anime anime = result.get();
            assertThat(anime.name()).isNotNull();
            assertThat(anime.publicId()).isNotNull();
        }
    }
}
