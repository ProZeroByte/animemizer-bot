package pro.zerobyte.animemizerbot;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
@Disabled("Integration test - requires live Shikimori API")
class RealSiteSessionTest {

    @Nested
    @DisplayName("fetchAnime")
    class FetchAnime {

        @Test
        void should_fetchAnime_when_realSiteAvailable() {
            // Given
            // Requires live external API - disabled for CI/CD

            // When
            // No execution - test is disabled

            // Then
            // Verification deferred until test is enabled
        }
    }
}
