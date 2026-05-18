package pro.zerobyte.animemizerbot.bot.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AnimemizerBotCallbackTest {

    @Nested
    @DisplayName("toCallbackData")
    class ToCallbackData {

        @Test
        void should_buildCorrectFormat_when_numericId() {
            // Given
            AnimemizerBotCallback callback = AnimemizerBotCallback.NEXT_EPISODE;

            // When
            String result = callback.toCallbackData(123);

            // Then
            assertThat(result).isEqualTo("NEXT_EPISODE:123");
        }

        @Test
        void should_appendPrefixAndColon() {
            // Given
            AnimemizerBotCallback callback = AnimemizerBotCallback.SET_WATCH_STATUS;

            // When
            String result = callback.toCallbackData("42");

            // Then
            assertThat(result).isEqualTo("SET_WATCH_STATUS:42");
            assertThat(result).startsWith(callback.getPrefix());
        }
    }

    @Nested
    @DisplayName("isCallbackSupported")
    class IsCallbackSupported {

        @Test
        void should_returnTrue_when_prefixMatches() {
            // Given
            AnimemizerBotCallback callback = AnimemizerBotCallback.LOAD_SEQUEL;

            // When
            boolean result = callback.isCallbackSupported("LOAD_SEQUEL:99");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnFalse_when_prefixDoesNotMatch() {
            // Given
            AnimemizerBotCallback callback = AnimemizerBotCallback.LOAD_SEQUEL;

            // When
            boolean result = callback.isCallbackSupported("NEXT_EPISODE:99");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void should_returnFalse_when_null() {
            // Given
            AnimemizerBotCallback callback = AnimemizerBotCallback.PREVIOUS_EPISODE;

            // When
            boolean result = callback.isCallbackSupported(null);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("extractId")
    class ExtractId {

        @Test
        void should_extractId_when_validCallbackData() {
            // Given
            AnimemizerBotCallback callback = AnimemizerBotCallback.NEXT_EPISODE;

            // When
            Long result = callback.extractId("NEXT_EPISODE:456");

            // Then
            assertThat(result).isEqualTo(456L);
        }

        @Test
        void should_returnNull_when_payloadIsEmpty() {
            // Given
            AnimemizerBotCallback callback = AnimemizerBotCallback.NEXT_EPISODE;

            // When
            Long result = callback.extractId("NEXT_EPISODE:");

            // Then
            assertThat(result).isNull();
        }

        @Test
        void should_returnNull_when_payloadIsNotNumeric() {
            // Given
            AnimemizerBotCallback callback = AnimemizerBotCallback.LOAD_PREQUEL;

            // When
            Long result = callback.extractId("LOAD_PREQUEL:abc");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        void should_parseValidCallbackData() {
            // Given
            String data = "NEXT_EPISODE:789";

            // When
            Optional<AnimemizerBotCallback.Payload> result = AnimemizerBotCallback.parse(data);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().action()).isEqualTo(AnimemizerBotCallback.NEXT_EPISODE);
            assertThat(result.get().id()).isEqualTo("789");
        }

        @Test
        void should_returnEmpty_when_null() {
            // Given
            String data = null;

            // When
            Optional<AnimemizerBotCallback.Payload> result = AnimemizerBotCallback.parse(data);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_noSeparator() {
            // Given
            String data = "NEXT_EPISODE";

            // When
            Optional<AnimemizerBotCallback.Payload> result = AnimemizerBotCallback.parse(data);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_unknownEnumName() {
            // Given
            String data = "UNKNOWN_ACTION:123";

            // When
            Optional<AnimemizerBotCallback.Payload> result = AnimemizerBotCallback.parse(data);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractWatchLinkPayload")
    class ExtractWatchLinkPayload {

        @Test
        void should_extractProviderCodeAndPublicId_when_validFormat() {
            // Given
            String id = "shikimori:12345";

            // When
            Optional<AnimemizerBotCallback.WatchLinkPayload> result = AnimemizerBotCallback.extractWatchLinkPayload(id);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().providerCode()).isEqualTo("shikimori");
            assertThat(result.get().publicId()).isEqualTo("12345");
        }

        @Test
        void should_returnEmpty_when_null() {
            // Given
            String id = null;

            // When
            Optional<AnimemizerBotCallback.WatchLinkPayload> result = AnimemizerBotCallback.extractWatchLinkPayload(id);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_noSeparator() {
            // Given
            String id = "nocolonhere";

            // When
            Optional<AnimemizerBotCallback.WatchLinkPayload> result = AnimemizerBotCallback.extractWatchLinkPayload(id);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
