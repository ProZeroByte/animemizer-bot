package pro.zerobyte.animemizerbot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import pro.zerobyte.animemizerbot.config.props.TelegramBotProperties;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeListService;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;
import pro.zerobyte.animemizerbot.core.watchlist.Watchlist;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimeListServiceTest {

    private static final Long USER_ID = 100L;
    private static final String BOT_USERNAME = "test_bot";

    @Mock
    private WatchlistService watchlistService;

    @Mock
    private AnimeService animeService;

    private AnimeListService listService;

    @BeforeEach
    void setUp() {
        listService = new AnimeListService(watchlistService, animeService,
                new TelegramBotProperties("token", BOT_USERNAME, null, null, null));
    }

    private Anime newAnime(Long id, String russianName, String name, Integer episodes) {
        return new Anime(id, "shikimori_" + id, null, null, name, russianName, null,
                null, "poster.jpg", "tv", false, false, true, episodes, null,
                null, null, null, null, null);
    }

    private Anime newAnimeWithPublicId(Long id, String publicId, String russianName, String name, Integer episodes) {
        return new Anime(id, publicId, null, null, name, russianName, null,
                null, "poster.jpg", "tv", false, false, true, episodes, null,
                null, null, null, null, null);
    }

    private Watchlist newWatchlist(Long animeId, WatchStatus status, Integer lastWatchedEpisode) {
        return new Watchlist(1L, USER_ID, animeId, status, lastWatchedEpisode,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("fetchWatchingEntries")
    class FetchWatchingEntries {

        @Test
        void should_filterByWatchingStatus_andPairWatchedEpisodes() {
            // Given
            Anime anime10 = newAnime(10L, "Первое", "First", 24);
            Anime anime12 = newAnime(12L, "Второе", "Second", 8);
            when(watchlistService.findByTelegramUserId(USER_ID)).thenReturn(List.of(
                    newWatchlist(10L, WatchStatus.WATCHING, 5),
                    newWatchlist(11L, WatchStatus.COMPLETED, 12),
                    newWatchlist(12L, WatchStatus.WATCHING, null)));
            when(animeService.findAllById(anyList())).thenReturn(List.of(anime10, anime12));

            // When
            List<AnimeListService.WatchingEntry> result = listService.fetchWatchingEntries(USER_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(AnimeListService.WatchingEntry::watchedEpisodes)
                    .containsExactlyInAnyOrder(5, 0);
            assertThat(result).extracting(entry -> entry.anime().id())
                    .containsExactlyInAnyOrder(10L, 12L);
        }

        @Test
        void should_returnEmpty_when_noWatchingEntries() {
            // Given
            when(watchlistService.findByTelegramUserId(USER_ID)).thenReturn(List.of(
                    newWatchlist(11L, WatchStatus.COMPLETED, 12),
                    newWatchlist(13L, WatchStatus.PLANNED, 0)));

            // When
            List<AnimeListService.WatchingEntry> result = listService.fetchWatchingEntries(USER_ID);

            // Then
            assertThat(result).isEmpty();
            verify(animeService, never()).findAllById(anyList());
        }

        @Test
        void should_returnEmpty_when_watchlistIsEmpty() {
            // Given
            when(watchlistService.findByTelegramUserId(USER_ID)).thenReturn(List.of());

            // When
            List<AnimeListService.WatchingEntry> result = listService.fetchWatchingEntries(USER_ID);

            // Then
            assertThat(result).isEmpty();
            verify(animeService, never()).findAllById(anyList());
        }

        @Test
        void should_sortByRussianNameIgnoringCase() {
            // Given
            Anime beta = newAnime(10L, "бета", "Beta", 12);
            Anime alpha = newAnime(11L, "Альфа", "Alpha", 12);
            when(watchlistService.findByTelegramUserId(USER_ID)).thenReturn(List.of(
                    newWatchlist(10L, WatchStatus.WATCHING, 1),
                    newWatchlist(11L, WatchStatus.WATCHING, 2)));
            when(animeService.findAllById(anyList())).thenReturn(List.of(beta, alpha));

            // When
            List<AnimeListService.WatchingEntry> result = listService.fetchWatchingEntries(USER_ID);

            // Then
            assertThat(result).extracting(entry -> entry.anime().russianName())
                    .containsExactly("Альфа", "бета");
        }

        @Test
        void should_fallbackToEnglishName_when_russianNameIsNull() {
            // Given
            Anime beta = newAnime(10L, null, "Beta", 12);
            Anime alpha = newAnime(11L, null, "Alpha", 12);
            when(watchlistService.findByTelegramUserId(USER_ID)).thenReturn(List.of(
                    newWatchlist(10L, WatchStatus.WATCHING, 1),
                    newWatchlist(11L, WatchStatus.WATCHING, 2)));
            when(animeService.findAllById(anyList())).thenReturn(List.of(beta, alpha));

            // When
            List<AnimeListService.WatchingEntry> result = listService.fetchWatchingEntries(USER_ID);

            // Then
            assertThat(result).extracting(entry -> entry.anime().name())
                    .containsExactly("Alpha", "Beta");
        }
    }

    @Nested
    @DisplayName("totalPages")
    class TotalPages {

        @Test
        void should_computePages_fromItemCount() {
            assertThat(listService.totalPages(0)).isZero();
            assertThat(listService.totalPages(1)).isEqualTo(1);
            assertThat(listService.totalPages(20)).isEqualTo(1);
            assertThat(listService.totalPages(21)).isEqualTo(2);
        }

        @Test
        void should_computePages_fromAnimeList() {
            // Given
            List<Anime> animes = new ArrayList<>();
            for (int i = 1; i <= 21; i++) {
                animes.add(newAnime((long) i, "Аниме " + i, "Anime " + i, 12));
            }

            // When / Then
            assertThat(listService.totalPages(animes)).isEqualTo(2);
            assertThat(listService.totalPages(List.of())).isZero();
        }
    }

    @Nested
    @DisplayName("buildWatchingPageText")
    class BuildWatchingPageText {

        @Test
        void should_showProgress_when_totalEpisodesKnown() {
            // Given
            List<AnimeListService.WatchingEntry> entries = List.of(
                    new AnimeListService.WatchingEntry(newAnime(10L, "Первое", "First", 24), 5));

            // When
            String text = listService.buildWatchingPageText(entries, 0);

            // Then
            assertThat(text).contains("[Первое](https://t.me/" + BOT_USERNAME + "?start=show_10)");
            assertThat(text).contains("5/24");
        }

        @Test
        void should_showQuestionMark_when_totalEpisodesUnknown() {
            // Given — ongoing anime without a final episode count
            List<AnimeListService.WatchingEntry> entries = List.of(
                    new AnimeListService.WatchingEntry(newAnime(10L, "Первое", "First", null), 5),
                    new AnimeListService.WatchingEntry(newAnime(11L, "Второе", "Second", 0), 3));

            // When
            String text = listService.buildWatchingPageText(entries, 0);

            // Then
            assertThat(text).contains("5/?");
            assertThat(text).contains("3/?");
        }

        @Test
        void should_escapeMarkdownInLinkText_when_titleHasSpecialCharacters() {
            // Given
            List<AnimeListService.WatchingEntry> entries = List.of(
                    new AnimeListService.WatchingEntry(newAnime(10L, "Первое [TV] *особое*", "First", 12), 5));

            // When
            String text = listService.buildWatchingPageText(entries, 0);

            // Then
            assertThat(text).contains("[Первое \\[TV\\] \\*особое\\*](https://t.me/" + BOT_USERNAME + "?start=show_10)");
        }

        @Test
        void should_showPlainTitle_when_publicIdIsNotShikimori() {
            // Given
            List<AnimeListService.WatchingEntry> entries = List.of(
                    new AnimeListService.WatchingEntry(
                            newAnimeWithPublicId(10L, "kino_123", "Первое", "First", 12), 5));

            // When
            String text = listService.buildWatchingPageText(entries, 0);

            // Then
            assertThat(text).contains("1. Первое — 5/12");
            assertThat(text).doesNotContain("](https://t.me/");
        }

        @Test
        void should_showEmptyMessage_when_noEntries() {
            // Given
            List<AnimeListService.WatchingEntry> entries = List.of();

            // When
            String text = listService.buildWatchingPageText(entries, 0);

            // Then
            assertThat(text).contains("Вы пока ничего не смотрите");
        }

        @Test
        void should_showPageIndicator_when_multiplePages() {
            // Given
            List<AnimeListService.WatchingEntry> entries = new ArrayList<>();
            for (int i = 1; i <= 21; i++) {
                entries.add(new AnimeListService.WatchingEntry(
                        newAnime((long) i, "Аниме " + i, "Anime " + i, 12), 1));
            }

            // When
            String firstPage = listService.buildWatchingPageText(entries, 0);
            String secondPage = listService.buildWatchingPageText(entries, 1);

            // Then
            assertThat(firstPage).contains("Страница 1/2");
            assertThat(secondPage).contains("Страница 2/2");
        }
    }

    @Nested
    @DisplayName("buildWatchingPageKeyboard")
    class BuildWatchingPageKeyboard {

        @Test
        void should_returnNull_when_singlePage() {
            // Given — anime links live in the text, so a single page needs no buttons at all
            List<AnimeListService.WatchingEntry> entries = List.of(
                    new AnimeListService.WatchingEntry(newAnime(10L, "Первое", "First", 24), 5),
                    new AnimeListService.WatchingEntry(newAnime(11L, "Второе", "Second", 12), 3));

            // When
            InlineKeyboardMarkup keyboard = listService.buildWatchingPageKeyboard(USER_ID, 0, 1);

            // Then
            assertThat(keyboard).isNull();
        }

        @Test
        void should_addOnlyPaginationButtons_when_multiplePages() {
            // Given
            List<AnimeListService.WatchingEntry> entries = new ArrayList<>();
            for (int i = 1; i <= 21; i++) {
                entries.add(new AnimeListService.WatchingEntry(
                        newAnime((long) i, "Аниме " + i, "Anime " + i, 12), 1));
            }

            // When
            InlineKeyboardMarkup firstPageKeyboard = listService.buildWatchingPageKeyboard(USER_ID, 0, 2);
            InlineKeyboardMarkup secondPageKeyboard = listService.buildWatchingPageKeyboard(USER_ID, 1, 2);

            // Then — one nav row per page, no per-anime buttons
            List<InlineKeyboardRow> firstPageRows = firstPageKeyboard.getKeyboard();
            assertThat(firstPageRows).hasSize(1);
            assertThat(firstPageRows.get(0)).hasSize(1);
            assertThat(firstPageRows.get(0).get(0).getText()).contains("Вперёд");
            assertThat(firstPageRows.get(0).get(0).getCallbackData()).isEqualTo("WATCHING_PAGE:100:1");

            List<InlineKeyboardRow> secondPageRows = secondPageKeyboard.getKeyboard();
            assertThat(secondPageRows).hasSize(1);
            assertThat(secondPageRows.get(0)).hasSize(1);
            assertThat(secondPageRows.get(0).get(0).getText()).contains("Назад");
            assertThat(secondPageRows.get(0).get(0).getCallbackData()).isEqualTo("WATCHING_PAGE:100:0");
        }

        @Test
        void should_returnNull_when_noPages() {
            // Given
            List<AnimeListService.WatchingEntry> entries = List.of();

            // When
            InlineKeyboardMarkup keyboard = listService.buildWatchingPageKeyboard(USER_ID, 0, 0);

            // Then
            assertThat(keyboard).isNull();
        }
    }
}
