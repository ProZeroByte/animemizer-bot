package pro.zerobyte.animemizerbot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;
import pro.zerobyte.animemizerbot.core.watchlist.Watchlist;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistRepository;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WatchlistServiceTest {

    private WatchlistRepository repository;
    private AnimeService animeService;
    private WatchlistService service;

    private static final Long USER_ID = 100L;
    private static final Long ANIME_ID = 200L;
    private static final LocalDateTime NOW = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        repository = mock(WatchlistRepository.class);
        animeService = mock(AnimeService.class);
        service = new WatchlistService(repository, animeService);
    }

    private Watchlist newWatchlist(Long userId, Long animeId, WatchStatus status, int lastWatchedEpisode) {
        return new Watchlist(1L, userId, animeId, status, lastWatchedEpisode, NOW, NOW);
    }

    private void stubSaveToReturnSame() {
        when(repository.save(any(Watchlist.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubAnimeEpisodes(int episodeCount) {
        Anime anime = new Anime(ANIME_ID, "test-anime", null, null, "Test Anime", "Тест", null,
                null, null, null, false, false, false, episodeCount, null, null, null, null, null, null, null);
        when(animeService.findById(ANIME_ID)).thenReturn(Optional.of(anime));
    }

    @Nested
    @DisplayName("getWatchlist")
    class GetWatchlist {

        @Test
        void should_returnWatchlist_when_entryExists() {
            // Given
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.WATCHING, 5);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));

            // When
            Optional<Watchlist> result = service.getWatchlist(USER_ID, ANIME_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().lastWatchedEpisode()).isEqualTo(5);
        }

        @Test
        void should_returnEmpty_when_entryDoesNotExist() {
            // Given
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.empty());

            // When
            Optional<Watchlist> result = service.getWatchlist(USER_ID, ANIME_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("incrementEpisode")
    class IncrementEpisode {

        @Test
        void should_createWatchlist_when_noneExists() {
            // Given
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.empty());
            stubSaveToReturnSame();

            // When
            Watchlist result = service.incrementEpisode(USER_ID, ANIME_ID, 12);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(1);
            assertThat(result.status()).isEqualTo(WatchStatus.WATCHING);
            verify(repository, atLeastOnce()).save(any(Watchlist.class));
        }

        @Test
        void should_createWatchlist_when_existingEntryExists() {
            // Given
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.WATCHING, 5);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));
            stubSaveToReturnSame();

            // When
            Watchlist result = service.incrementEpisode(USER_ID, ANIME_ID, 12);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(6);
        }

        @Test
        void should_returnExistingEntry_when_oneAlreadyExists() {
            // Given
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.WATCHING, 12);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));

            // When
            Watchlist result = service.incrementEpisode(USER_ID, ANIME_ID, 12);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(12);
            verify(repository, never()).save(any());
        }

        @Test
        void should_setStatusToCompleted_when_allEpisodesWatched() {
            // Given
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.WATCHING, 11);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));
            stubSaveToReturnSame();

            // When
            Watchlist result = service.incrementEpisode(USER_ID, ANIME_ID, 12);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(12);
            assertThat(result.status()).isEqualTo(WatchStatus.COMPLETED);
        }

        @Test
        void should_setStatusToOngoing_when_notAllEpisodesWatched() {
            // Given
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.PLANNED, 4);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));
            stubSaveToReturnSame();

            // When
            Watchlist result = service.incrementEpisode(USER_ID, ANIME_ID, 12);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(5);
            assertThat(result.status()).isEqualTo(WatchStatus.PLANNED);
        }

        @Test
        void should_notChangeStatus_when_episodeCountIsZero() {
            // Given
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.PLANNED, 0);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));
            stubSaveToReturnSame();

            // When
            Watchlist result = service.incrementEpisode(USER_ID, ANIME_ID, 12);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(1);
            assertThat(result.status()).isEqualTo(WatchStatus.PLANNED);
        }
    }

    @Nested
    @DisplayName("decreaseEpisode")
    class DecreaseEpisode {

        @Test
        void should_decreaseEpisodeCorrectly_when_decreasing() {
            // Given
            stubAnimeEpisodes(12);
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.COMPLETED, 12);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));
            stubSaveToReturnSame();

            // When
            Watchlist result = service.decreaseEpisode(USER_ID, ANIME_ID);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(11);
            assertThat(result.status()).isEqualTo(WatchStatus.WATCHING);
        }

        @Test
        void should_notDecreaseBelowZero_when_onZero() {
            // Given
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.WATCHING, 0);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));

            // When
            Watchlist result = service.decreaseEpisode(USER_ID, ANIME_ID);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(0);
            assertThat(result.status()).isEqualTo(WatchStatus.WATCHING);
            verify(repository, never()).save(any());
        }

        @Test
        void should_revertStatusOnlyBelowTotal_when_decreasingFromCompleted() {
            // Given - anime has 12 episodes, watchlist is at 12 COMPLETED
            stubAnimeEpisodes(12);
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.COMPLETED, 12);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));
            stubSaveToReturnSame();

            // When
            Watchlist result = service.decreaseEpisode(USER_ID, ANIME_ID);

            // Then - 11 < 12, so status reverts to WATCHING
            assertThat(result.lastWatchedEpisode()).isEqualTo(11);
            assertThat(result.status()).isEqualTo(WatchStatus.WATCHING);
        }

        @Test
        void should_notRevertStatus_when_notCompleted() {
            // Given
            stubAnimeEpisodes(12);
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.WATCHING, 5);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));
            stubSaveToReturnSame();

            // When
            Watchlist result = service.decreaseEpisode(USER_ID, ANIME_ID);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(4);
            assertThat(result.status()).isEqualTo(WatchStatus.WATCHING);
        }

        @Test
        void should_createWatchlist_when_notPresent() {
            // Given
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.empty());
            stubSaveToReturnSame();

            // When
            Watchlist result = service.decreaseEpisode(USER_ID, ANIME_ID);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(0);
            assertThat(result.status()).isEqualTo(WatchStatus.WATCHING);
        }
    }

    @Nested
    @DisplayName("setStatus")
    class SetStatus {

        @Test
        void should_setEpisodeToMax_when_statusSetToCompleted() {
            // Given
            stubAnimeEpisodes(12);
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.WATCHING, 5);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));
            stubSaveToReturnSame();

            // When
            Watchlist result = service.setStatus(USER_ID, ANIME_ID, WatchStatus.COMPLETED);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(12);
            assertThat(result.status()).isEqualTo(WatchStatus.COMPLETED);
        }

        @Test
        void should_keepCurrentEpisode_when_completedAndEpisodeCountIsZero() {
            // Given
            stubAnimeEpisodes(0);
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.WATCHING, 5);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));
            stubSaveToReturnSame();

            // When
            Watchlist result = service.setStatus(USER_ID, ANIME_ID, WatchStatus.COMPLETED);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(5);
            assertThat(result.status()).isEqualTo(WatchStatus.COMPLETED);
        }

        @Test
        void should_keepCurrentEpisode_when_completedAndEpisodeCountIsNull() {
            // Given
            Anime anime = new Anime(ANIME_ID, "test-anime", null, null, "Test Anime", "Тест", null,
                    null, null, null, false, false, false, null, null, null, null, null, null, null, null);
            when(animeService.findById(ANIME_ID)).thenReturn(Optional.of(anime));
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.WATCHING, 5);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));
            stubSaveToReturnSame();

            // When
            Watchlist result = service.setStatus(USER_ID, ANIME_ID, WatchStatus.COMPLETED);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(5);
            assertThat(result.status()).isEqualTo(WatchStatus.COMPLETED);
        }

        @Test
        void should_notChangeEpisode_when_statusNotCompleted() {
            // Given
            stubAnimeEpisodes(12);
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.WATCHING, 5);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));
            stubSaveToReturnSame();

            // When
            Watchlist result = service.setStatus(USER_ID, ANIME_ID, WatchStatus.PLANNED);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(5);
            assertThat(result.status()).isEqualTo(WatchStatus.PLANNED);
        }

        @Test
        void should_notLookupAnime_when_statusNotCompleted() {
            // Given
            Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.WATCHING, 5);
            when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));
            stubSaveToReturnSame();

            // When
            Watchlist result = service.setStatus(USER_ID, ANIME_ID, WatchStatus.ON_HOLD);

            // Then
            assertThat(result.lastWatchedEpisode()).isEqualTo(5);
            verify(animeService, never()).findById(any());
        }
    }
}
