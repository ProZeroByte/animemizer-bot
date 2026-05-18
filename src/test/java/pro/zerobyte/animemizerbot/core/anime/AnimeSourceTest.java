package pro.zerobyte.animemizerbot.core.anime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AnimeSourceTest {

    @Nested
    @DisplayName("createPublicId")
    class CreatePublicId {

        @Test
        void should_returnPrefixedId_when_shikimori() {
            // Given
            AnimeSource source = AnimeSource.SHIKIMORI;

            // When
            String result = source.createPublicId(123);

            // Then
            assertThat(result).isEqualTo("shikimori_123");
        }

        @Test
        void should_returnPrefixedId_when_anilibria() {
            // Given
            AnimeSource source = AnimeSource.ANILIBRIA;

            // When
            String result = source.createPublicId(456);

            // Then
            assertThat(result).isEqualTo("anilibria_456");
        }

        @Test
        void should_returnPrefixedId_when_animego() {
            // Given
            AnimeSource source = AnimeSource.ANIMEGO;

            // When
            String result = source.createPublicId(789);

            // Then
            assertThat(result).isEqualTo("animego_789");
        }

        @Test
        void should_returnPrefixedId_when_yummyanime() {
            // Given
            AnimeSource source = AnimeSource.YUMMYANIME;

            // When
            String result = source.createPublicId(101);

            // Then
            assertThat(result).isEqualTo("yummyanime_101");
        }
    }

    @Nested
    @DisplayName("extractIdFromPublicId")
    class ExtractIdFromPublicId {

        @Test
        void should_extractId_when_validPublicId() {
            // Given
            AnimeSource source = AnimeSource.SHIKIMORI;
            String publicId = "shikimori_55644";

            // When
            Optional<Long> result = source.extractIdFromPublicId(publicId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(55644L);
        }

        @Test
        void should_returnEmpty_when_null() {
            // Given
            AnimeSource source = AnimeSource.SHIKIMORI;

            // When
            Optional<Long> result = source.extractIdFromPublicId(null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_noPrefix() {
            // Given
            AnimeSource source = AnimeSource.SHIKIMORI;
            String publicId = "short";

            // When
            Optional<Long> result = source.extractIdFromPublicId(publicId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_nonNumericSuffix() {
            // Given
            AnimeSource source = AnimeSource.ANIMEGO;
            String publicId = "animego_abc";

            // When
            Optional<Long> result = source.extractIdFromPublicId(publicId);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
