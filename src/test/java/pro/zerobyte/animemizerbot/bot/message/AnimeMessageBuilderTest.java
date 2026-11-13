package pro.zerobyte.animemizerbot.bot.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnimeMessageBuilderTest {

    private final AnimeMessageBuilder builder = new AnimeMessageBuilder();

    private Anime newAnime(String name, String russianName, Integer episodes,
                           String prequelPublicId, String sequelPublicId) {
        return newAnime(name, russianName, episodes, prequelPublicId, sequelPublicId, null);
    }

    private Anime newAnime(String name, String russianName, Integer episodes,
                           String prequelPublicId, String sequelPublicId, String watchUrl) {
        return new Anime(1L, "shikimori_1", prequelPublicId, sequelPublicId, name, russianName, null,
                "desc", "poster.jpg", "tv", true, false, false, episodes, episodes,
                LocalDate.now(), LocalDate.now(), LocalDateTime.now(), null, watchUrl);
    }

    private Anime newOngoingAnime(String name, String russianName) {
        return new Anime(1L, "shikimori_1", null, null, name, russianName, null,
                "desc", "poster.jpg", "tv", false, false, true, 12, 8,
                LocalDate.now(), null, LocalDateTime.now(), null, null);
    }

    @Nested
    @DisplayName("buildCaption")
    class BuildCaption {

        @Test
        void should_includeRussianName_when_available() {
            // Given
            Anime anime = newAnime("Test Anime", "Тестовое аниме", 12, null, null);

            // When
            String caption = builder.buildCaption(anime, 0, null);

            // Then
            assertThat(caption).contains("Тестовое аниме");
        }

        @Test
        void should_fallbackToName_when_russianNameIsNull() {
            // Given
            Anime anime = newAnime("Test Anime", null, 12, null, null);

            // When
            String caption = builder.buildCaption(anime, 0, null);

            // Then
            assertThat(caption).contains("Test Anime");
        }

        @Test
        void should_showQuestionMark_when_episodesIsNull() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", null, null, null);

            // When
            String caption = builder.buildCaption(anime, 0, null);

            // Then
            assertThat(caption).contains("?");
        }

        @Test
        void should_includeWatchStatus_when_provided() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null);

            // When
            String caption = builder.buildCaption(anime, 0, WatchStatus.WATCHING);

            // Then
            assertThat(caption).contains("Смотрю");
        }

        @Test
        void should_notIncludeWatchStatus_when_null() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null);

            // When
            String caption = builder.buildCaption(anime, 0, null);

            // Then
            assertThat(caption).doesNotContain("Статус");
        }
    }

    @Nested
    @DisplayName("createInlineKeyboard")
    class CreateInlineKeyboard {

        @Test
        void should_containEpisodeButtons_when_notAllWatched() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null);

            // When
            InlineKeyboardMarkup markup = builder.createInlineKeyboard(anime, 5);

            // Then
            List<InlineKeyboardRow> rows = markup.getKeyboard();
            List<InlineKeyboardButton> episodeRow = rows.get(0);
            assertThat(episodeRow).hasSize(2);
            assertThat(episodeRow.stream().map(InlineKeyboardButton::getText)).containsExactly("-1 серия", "+1 серия");
        }

        @Test
        void should_containOnlyMinusButton_when_allEpisodesWatched() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null);

            // When
            InlineKeyboardMarkup markup = builder.createInlineKeyboard(anime, 12);

            // Then
            List<InlineKeyboardRow> rows = markup.getKeyboard();
            List<InlineKeyboardButton> episodeRow = rows.get(0);
            assertThat(episodeRow).hasSize(1);
            assertThat(episodeRow.get(0).getText()).isEqualTo("-1 серия");
        }

        @Test
        void should_containPrequelButton_when_prequelExists() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", 12, "shikimori_999", null);

            // When
            InlineKeyboardMarkup markup = builder.createInlineKeyboard(anime, 0);

            // Then
            List<InlineKeyboardRow> rows = markup.getKeyboard();
            boolean hasPrequelButton = rows.stream()
                    .flatMap(List::stream)
                    .anyMatch(btn -> btn.getText().contains("Предыстория"));
            assertThat(hasPrequelButton).isTrue();
        }

        @Test
        void should_containSequelButton_when_sequelExists() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", 12, null, "shikimori_1001");

            // When
            InlineKeyboardMarkup markup = builder.createInlineKeyboard(anime, 0);

            // Then
            List<InlineKeyboardRow> rows = markup.getKeyboard();
            boolean hasSequelButton = rows.stream()
                    .flatMap(List::stream)
                    .anyMatch(btn -> btn.getText().contains("Продолжение"));
            assertThat(hasSequelButton).isTrue();
        }

        @Test
        void should_containStatusButtons() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null);

            // When
            InlineKeyboardMarkup markup = builder.createInlineKeyboard(anime, 0);

            // Then
            List<InlineKeyboardRow> rows = markup.getKeyboard();
            List<String> allButtonTexts = rows.stream()
                    .flatMap(List::stream)
                    .map(InlineKeyboardButton::getText)
                    .toList();
            assertThat(allButtonTexts).contains("👀", "📋", "✅", "⏸", "❌");
        }

        @Test
        void should_placeRefreshButtonInSecondStatusRowFirstSlot() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null);

            // When
            InlineKeyboardMarkup markup = builder.createInlineKeyboard(anime, 0);

            // Then
            List<InlineKeyboardRow> rows = markup.getKeyboard();
            InlineKeyboardRow firstStatusRow = rows.get(1);
            InlineKeyboardRow secondStatusRow = rows.get(2);

            assertThat(firstStatusRow.stream().map(InlineKeyboardButton::getText))
                    .containsExactly("👀", "📋", "✅");
            assertThat(secondStatusRow.stream().map(InlineKeyboardButton::getText))
                    .containsExactly("🔄", "⏸", "❌");
            assertThat(secondStatusRow.get(0).getCallbackData()).isEqualTo("REFRESH:1");
        }

        @Test
        void should_containRefreshButton_when_animeIsOngoing() {
            // Given
            Anime anime = newOngoingAnime("Test Anime", "Тест");

            // When
            InlineKeyboardMarkup markup = builder.createInlineKeyboard(anime, 0);

            // Then
            List<InlineKeyboardRow> rows = markup.getKeyboard();
            InlineKeyboardRow secondStatusRow = rows.get(2);
            assertThat(secondStatusRow.get(0).getText()).isEqualTo("🔄");
            assertThat(secondStatusRow.get(0).getCallbackData()).isEqualTo("REFRESH:1");
        }

        @Test
        void should_notContainRewatchingStatusButton() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null);

            // When
            InlineKeyboardMarkup markup = builder.createInlineKeyboard(anime, 0);

            // Then
            List<InlineKeyboardButton> allButtons = markup.getKeyboard().stream()
                    .flatMap(List::stream)
                    .toList();
            assertThat(allButtons)
                    .noneMatch(btn -> "SET_WATCH_STATUS:1:REWATCHING".equals(btn.getCallbackData()));
        }

        @Test
        void should_containWatchButton_when_watchUrlExists() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null,
                    "https://www.kinokino.red/series/123/");

            // When
            InlineKeyboardMarkup markup = builder.createInlineKeyboard(anime, 0);

            // Then
            List<InlineKeyboardRow> rows = markup.getKeyboard();
            List<InlineKeyboardButton> urlButtons = rows.stream()
                    .flatMap(List::stream)
                    .filter(btn -> btn.getUrl() != null)
                    .toList();
            assertThat(urlButtons).hasSize(1);
            assertThat(urlButtons.get(0).getText()).isEqualTo("Смотреть");
            assertThat(urlButtons.get(0).getUrl()).isEqualTo("https://www.kinokino.red/series/123/");
        }

        @Test
        void should_notContainWatchButton_when_noWatchUrl() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null);

            // When
            InlineKeyboardMarkup markup = builder.createInlineKeyboard(anime, 0);

            // Then
            List<InlineKeyboardRow> rows = markup.getKeyboard();
            boolean hasUrlButton = rows.stream()
                    .flatMap(List::stream)
                    .anyMatch(btn -> btn.getUrl() != null);
            assertThat(hasUrlButton).isFalse();
        }
    }
}
