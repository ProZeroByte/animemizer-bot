package pro.zerobyte.animemizerbot.config.props;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShikimoriPropertiesTest {

    @Test
    void should_useDefaultBaseUrl_when_baseUrlIsMissing() {
        // Given
        ShikimoriProperties properties = new ShikimoriProperties(null);

        // When
        String baseUrl = properties.resolvedBaseUrl();

        // Then
        assertThat(baseUrl).isEqualTo("https://shikimori.io");
    }

    @Test
    void should_useDefaultBaseUrl_when_baseUrlIsBlank() {
        // Given
        ShikimoriProperties properties = new ShikimoriProperties(" ");

        // When
        String baseUrl = properties.resolvedBaseUrl();

        // Then
        assertThat(baseUrl).isEqualTo("https://shikimori.io");
    }
}