package pro.zerobyte.animemizerbot.bot.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.zerobyte.animemizerbot.config.props.AnimeUrlProperties;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeLink;
import pro.zerobyte.animemizerbot.core.anime.AnimeSource;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AnimeMessageBuilderTest {

    private static final AnimeUrlProperties URL_PROPERTIES = new AnimeUrlProperties(
            Map.of(AnimeSource.SHIKIMORI, "https://shikimori.one"),
            "https://animemizer.bot/watch?source=%s&publicId=%s"
    );

    private final AnimeMessageBuilder builder = new AnimeMessageBuilder(URL_PROPERTIES);

    private Anime newAnime(String name, String russianName, Integer episodes,
                           String prequelPublicId, String sequelPublicId, Set<AnimeLink> links) {
        return new Anime(1L, "shikimori_1", prequelPublicId, sequelPublicId, name, russianName, null,
                "desc", "poster.jpg", "tv", true, false, false, episodes, episodes,
                LocalDate.now(), LocalDate.now(), LocalDateTime.now(), null, links);
    }

    @Nested
    @DisplayName("buildCaption")
    class BuildCaption {

        @Test
        void should_includeRussianName_when_available() {
            // Given
            Anime anime = newAnime("Test Anime", "Тестовое аниме", 12, null, null, Set.of());

            // When
            String caption = builder.buildCaption(anime, 0, null);

            // Then
            assertThat(caption).contains("Тестовое аниме");
        }

        @Test
        void should_fallbackToName_when_russianNameIsNull() {
            // Given
            Anime anime = newAnime("Test Anime", null, 12, null, null, Set.of());

            // When
            String caption = builder.buildCaption(anime, 0, null);

            // Then
            assertThat(caption).contains("Test Anime");
        }

        @Test
        void should_showQuestionMark_when_episodesIsNull() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", null, null, null, Set.of());

            // When
            String caption = builder.buildCaption(anime, 0, null);

            // Then
            assertThat(caption).contains("?");
        }

        @Test
        void should_includeWatchStatus_when_provided() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null, Set.of());

            // When
            String caption = builder.buildCaption(anime, 0, WatchStatus.WATCHING);

            // Then
            assertThat(caption).contains("Смотрю");
        }

        @Test
        void should_notIncludeWatchStatus_when_null() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null, Set.of());

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
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null, Set.of());

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
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null, Set.of());

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
            Anime anime = newAnime("Test Anime", "Тест", 12, "shikimori_999", null, Set.of());

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
            Anime anime = newAnime("Test Anime", "Тест", 12, null, "shikimori_1001", Set.of());

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
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null, Set.of());

            // When
            InlineKeyboardMarkup markup = builder.createInlineKeyboard(anime, 0);

            // Then
            List<InlineKeyboardRow> rows = markup.getKeyboard();
            List<String> allButtonTexts = rows.stream()
                    .flatMap(List::stream)
                    .map(InlineKeyboardButton::getText)
                    .toList();
            assertThat(allButtonTexts).contains("👀", "📋", "✅");
        }

        @Test
        void should_containWatchLinkButtons_when_linksExist() {
            // Given
            AnimeLink link = new AnimeLink(1L, 1L, AnimeSource.ANILIBRIA, "anime/releases/release/123");
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null, Set.of(link));

            // When
            InlineKeyboardMarkup markup = builder.createInlineKeyboard(anime, 0);

            // Then
            List<InlineKeyboardRow> rows = markup.getKeyboard();
            List<InlineKeyboardButton> urlButtons = rows.stream()
                    .flatMap(List::stream)
                    .filter(btn -> btn.getUrl() != null)
                    .toList();
            assertThat(urlButtons).isNotEmpty();
            assertThat(urlButtons.get(0).getText()).isEqualTo("▶️ AniLiberty");
            assertThat(urlButtons.get(0).getUrl()).contains("ANILIBRIA");
        }

        @Test
        void should_notContainWatchLinkButtons_when_noLinks() {
            // Given
            Anime anime = newAnime("Test Anime", "Тест", 12, null, null, Set.of());

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
