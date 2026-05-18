package pro.zerobyte.animemizerbot.core.watchlist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WatchStatusTest {

    @Nested
    @DisplayName("isTerminal")
    class IsTerminal {

        @Test
        void should_returnTrue_when_completed() {
            // Given
            WatchStatus status = WatchStatus.COMPLETED;

            // When
            boolean result = status.isTerminal();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnTrue_when_dropped() {
            // Given
            WatchStatus status = WatchStatus.DROPPED;

            // When
            boolean result = status.isTerminal();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnFalse_when_watching() {
            // Given
            WatchStatus status = WatchStatus.WATCHING;

            // When
            boolean result = status.isTerminal();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void should_returnFalse_when_planned() {
            // Given
            WatchStatus status = WatchStatus.PLANNED;

            // When
            boolean result = status.isTerminal();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void should_returnFalse_when_rewatching() {
            // Given
            WatchStatus status = WatchStatus.REWATCHING;

            // When
            boolean result = status.isTerminal();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void should_returnFalse_when_onHold() {
            // Given
            WatchStatus status = WatchStatus.ON_HOLD;

            // When
            boolean result = status.isTerminal();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("onAllEpisodesWatched")
    class OnAllEpisodesWatched {

        @Test
        void should_returnCompleted_when_watching() {
            // Given
            WatchStatus status = WatchStatus.WATCHING;

            // When
            WatchStatus result = status.onAllEpisodesWatched();

            // Then
            assertThat(result).isEqualTo(WatchStatus.COMPLETED);
        }

        @Test
        void should_returnCompleted_when_rewatching() {
            // Given
            WatchStatus status = WatchStatus.REWATCHING;

            // When
            WatchStatus result = status.onAllEpisodesWatched();

            // Then
            assertThat(result).isEqualTo(WatchStatus.COMPLETED);
        }

        @Test
        void should_returnSame_when_planned() {
            // Given
            WatchStatus status = WatchStatus.PLANNED;

            // When
            WatchStatus result = status.onAllEpisodesWatched();

            // Then
            assertThat(result).isEqualTo(WatchStatus.PLANNED);
        }

        @Test
        void should_returnSame_when_dropped() {
            // Given
            WatchStatus status = WatchStatus.DROPPED;

            // When
            WatchStatus result = status.onAllEpisodesWatched();

            // Then
            assertThat(result).isEqualTo(WatchStatus.DROPPED);
        }

        @Test
        void should_returnSame_when_onHold() {
            // Given
            WatchStatus status = WatchStatus.ON_HOLD;

            // When
            WatchStatus result = status.onAllEpisodesWatched();

            // Then
            assertThat(result).isEqualTo(WatchStatus.ON_HOLD);
        }
    }

    @Nested
    @DisplayName("onRewatchStarted")
    class OnRewatchStarted {

        @Test
        void should_returnRewatching_when_completed() {
            // Given
            WatchStatus status = WatchStatus.COMPLETED;

            // When
            WatchStatus result = status.onRewatchStarted();

            // Then
            assertThat(result).isEqualTo(WatchStatus.REWATCHING);
        }

        @Test
        void should_returnSame_when_watching() {
            // Given
            WatchStatus status = WatchStatus.WATCHING;

            // When
            WatchStatus result = status.onRewatchStarted();

            // Then
            assertThat(result).isEqualTo(WatchStatus.WATCHING);
        }

        @Test
        void should_returnSame_when_planned() {
            // Given
            WatchStatus status = WatchStatus.PLANNED;

            // When
            WatchStatus result = status.onRewatchStarted();

            // Then
            assertThat(result).isEqualTo(WatchStatus.PLANNED);
        }
    }
}
