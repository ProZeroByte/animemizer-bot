package pro.zerobyte.animemizerbot.bot.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramMessageUtilsTest {

    @Nested
    @DisplayName("escapeMarkdown")
    class EscapeMarkdown {

        @Test
        void should_returnEmpty_when_textIsNull() {
            // Given
            String text = null;

            // When
            String result = TelegramMessageUtils.escapeMarkdown(text);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_textIsBlank() {
            // Given
            String text = "   ";

            // When
            String result = TelegramMessageUtils.escapeMarkdown(text);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_notEscapeHyphens_when_textContainsDashes() {
            // Given
            String text = "Рыцарь-скелет вступает в параллельный мир";

            // When
            String result = TelegramMessageUtils.escapeMarkdown(text);

            // Then
            assertThat(result).isEqualTo("Рыцарь-скелет вступает в параллельный мир");
        }

        @Test
        void should_notEscapeDotsAndExclamations_when_textContainsPunctuation() {
            // Given
            String text = "Привет! Как дела? Ок. Да — нет.";

            // When
            String result = TelegramMessageUtils.escapeMarkdown(text);

            // Then
            assertThat(result).isEqualTo("Привет! Как дела? Ок. Да — нет.");
        }

        @Test
        void should_escapeUnderscore_when_textContainsIt() {
            // Given
            String text = "Hello_World";

            // When
            String result = TelegramMessageUtils.escapeMarkdown(text);

            // Then
            assertThat(result).isEqualTo("Hello\\_World");
        }

        @Test
        void should_escapeAsterisk_when_textContainsIt() {
            // Given
            String text = "a * b";

            // When
            String result = TelegramMessageUtils.escapeMarkdown(text);

            // Then
            assertThat(result).isEqualTo("a \\* b");
        }

        @Test
        void should_escapeBracketsAndParentheses_when_textContainsLinkSyntax() {
            // Given
            String text = "[link](url)";

            // When
            String result = TelegramMessageUtils.escapeMarkdown(text);

            // Then
            assertThat(result).isEqualTo("\\[link\\]\\(url\\)");
        }

        @Test
        void should_escapeBacktick_when_textContainsIt() {
            // Given
            String text = "Use `code` here";

            // When
            String result = TelegramMessageUtils.escapeMarkdown(text);

            // Then
            assertThat(result).isEqualTo("Use \\`code\\` here");
        }

        @Test
        void should_escapeBackslash_when_textContainsIt() {
            // Given
            String text = "path\\to\\file";

            // When
            String result = TelegramMessageUtils.escapeMarkdown(text);

            // Then
            assertThat(result).isEqualTo("path\\\\to\\\\file");
        }

        @Test
        void should_preserveUrlWithDashes_when_textContainsUrl() {
            // Given
            String text = "http://ru.wikipedia.org/wiki/Суплес_(борьба)";

            // When
            String result = TelegramMessageUtils.escapeMarkdown(text);

            // Then
            assertThat(result).isEqualTo("http://ru.wikipedia.org/wiki/Суплес\\_\\(борьба\\)");
        }

        @Test
        void should_notEscapeHashPlusEqualsPipe_when_textContainsV2OnlyChars() {
            // Given
            String text = "a+b=c#1 {ok} | x>0 ~done";

            // When
            String result = TelegramMessageUtils.escapeMarkdown(text);

            // Then
            assertThat(result).isEqualTo("a+b=c#1 {ok} | x>0 ~done");
        }
    }

    @Nested
    @DisplayName("escapeHtml")
    class EscapeHtml {

        @Test
        void should_returnEmpty_when_textIsNull() {
            // Given
            String text = null;

            // When
            String result = TelegramMessageUtils.escapeHtml(text);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_textIsBlank() {
            // Given
            String text = "   ";

            // When
            String result = TelegramMessageUtils.escapeHtml(text);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_notChange_when_plainText() {
            // Given
            String text = "Врата Штейна: начало — 24 серии";

            // When
            String result = TelegramMessageUtils.escapeHtml(text);

            // Then
            assertThat(result).isEqualTo(text);
        }

        @Test
        void should_escapeAmpersand_when_textContainsIt() {
            // Given
            String text = "Ранобэ & экранизация";
            String ampEntity = '&' + "amp;";

            // When
            String result = TelegramMessageUtils.escapeHtml(text);

            // Then
            assertThat(result).isEqualTo("Ранобэ " + ampEntity + " экранизация");
        }

        @Test
        void should_escapeAngleBrackets_when_textContainsTagLikeText() {
            // Given
            String text = "Блич <ТВ-3> и продолжение";
            String ltEntity = '&' + "lt;";
            String gtEntity = '&' + "gt;";

            // When
            String result = TelegramMessageUtils.escapeHtml(text);

            // Then
            assertThat(result).isEqualTo("Блич " + ltEntity + "ТВ-3" + gtEntity + " и продолжение");
        }

        @Test
        void should_doubleEscape_when_textContainsEntityLikeSequence() {
            // Given — already escaped text must not pass through as markup
            String ampEntity = '&' + "amp;";
            String text = "A " + ampEntity + " B";

            // When
            String result = TelegramMessageUtils.escapeHtml(text);

            // Then
            assertThat(result).isEqualTo("A " + ampEntity + "amp; B");
        }

        @Test
        void should_notEscapeQuotes_when_textContainsQuotedText() {
            // Given — Telegram HTML does not require quotes to be escaped in text
            String text = "Аниме \"Твоя апрельская ложь\"";

            // When
            String result = TelegramMessageUtils.escapeHtml(text);

            // Then
            assertThat(result).isEqualTo(text);
        }
    }

    @Nested
    @DisplayName("cleanShikimoriMarkup")
    class CleanShikimoriMarkup {

        @Test
        void should_returnEmpty_when_textIsNull() {
            // Given
            String text = null;

            // When
            String result = TelegramMessageUtils.cleanShikimoriMarkup(text);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_unwrapCharacterLinks_when_textContainsShikimoriMarkup() {
            // Given
            String text = "[character=7407]Наруто[/character]";

            // When
            String result = TelegramMessageUtils.cleanShikimoriMarkup(text);

            // Then
            assertThat(result).isEqualTo("Наруто");
        }

        @Test
        void should_unwrapAnimeLinks_when_textContainsShikimoriMarkup() {
            // Given
            String text = "[anime=1234]Test Anime[/anime]";

            // When
            String result = TelegramMessageUtils.cleanShikimoriMarkup(text);

            // Then
            assertThat(result).isEqualTo("Test Anime");
        }

        @Test
        void should_removeJapaneseBrackets_when_textContainsJapaneseNames() {
            // Given
            String text = "Наруто [うちは サスケ] тест";

            // When
            String result = TelegramMessageUtils.cleanShikimoriMarkup(text);

            // Then
            assertThat(result).isEqualTo("Наруто тест");
        }

        @Test
        void should_removeBBCode_when_textContainsFormattingTags() {
            // Given
            String text = "[b]жирный[/b] и [i]курсив[/i]";

            // When
            String result = TelegramMessageUtils.cleanShikimoriMarkup(text);

            // Then
            assertThat(result).isEqualTo("жирный и курсив");
        }
    }

    @Nested
    @DisplayName("truncateText")
    class TruncateText {

        @Test
        void should_returnEmpty_when_textIsNull() {
            // Given
            String text = null;

            // When
            String result = TelegramMessageUtils.truncateText(text, 10);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnSameText_when_textIsWithinLimit() {
            // Given
            String text = "Short text";

            // When
            String result = TelegramMessageUtils.truncateText(text, 100);

            // Then
            assertThat(result).isEqualTo("Short text");
        }

        @Test
        void should_appendEllipsis_when_textExceedsLimit() {
            // Given
            String text = "Это очень длинный текст, который нужно обрезать";

            // When
            String result = TelegramMessageUtils.truncateText(text, 10);

            // Then
            assertThat(result).hasSize(10);
            assertThat(result).endsWith("...");
        }
    }

    @Nested
    @DisplayName("cleanDescription")
    class CleanDescription {

        @Test
        void should_returnEmpty_when_textIsNull() {
            // Given
            String text = null;

            // When
            String result = TelegramMessageUtils.cleanDescription(text);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_textIsBlank() {
            // Given
            String text = "   ";

            // When
            String result = TelegramMessageUtils.cleanDescription(text);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_stripCharacterLinksAndCleanMarkup_when_textContainsShikimoriDescription() {
            // Given
            String text = "Чжоу Минжуй просыпается не в своём теле. Его новое имя — [character=233866]Клейн Моретти[/character], и он живёт в мире, где магия и наука идут рука об руку. "
                    + "[character=233866]Клейн[/character] сделал выбор — не бежать, а взглянуть в самую суть мира.";

            // When
            String result = TelegramMessageUtils.cleanDescription(text);

            // Then
            assertThat(result).doesNotContain("[character=");
            assertThat(result).doesNotContain("[/character]");
            assertThat(result).contains("Клейн Моретти");
            assertThat(result).contains("Клейн");
            assertThat(result).contains("Чжоу Минжуй");
        }

        @Test
        void should_stripUrls_when_textContainsHttpLinks() {
            // Given
            String text = "Описание https://example.com/test и http://shikimori.one/anime/123 конец.";

            // When
            String result = TelegramMessageUtils.cleanDescription(text);

            // Then
            assertThat(result).doesNotContain("https://");
            assertThat(result).doesNotContain("http://");
            assertThat(result).contains("Описание");
            assertThat(result).contains("конец.");
        }

        @Test
        void should_stripGenericBBCode_when_textContainsUnknownTags() {
            // Given
            String text = "[spoiler]секрет[/spoiler] обычный текст [custom]ещё[/custom]";

            // When
            String result = TelegramMessageUtils.cleanDescription(text);

            // Then
            assertThat(result).doesNotContain("[spoiler]");
            assertThat(result).doesNotContain("[/spoiler]");
            assertThat(result).doesNotContain("[custom]");
            assertThat(result).contains("секрет");
            assertThat(result).contains("обычный текст");
            assertThat(result).contains("ещё");
        }

        @Test
        void should_stripJapaneseCharacters_when_textContainsCJK() {
            // Given
            String text = "Аниме テストアニメ うちは описывает историю.";

            // When
            String result = TelegramMessageUtils.cleanDescription(text);

            // Then
            assertThat(result).doesNotContain("テストアニメ");
            assertThat(result).doesNotContain("うちは");
            assertThat(result).contains("Аниме");
            assertThat(result).contains("описывает историю.");
        }

        @Test
        void should_normalizeWhitespace_when_textHasMultipleSpacesAndNewlines() {
            // Given
            String text = "Первый   абзац.\n\n\n\nВторой   абзац.";

            // When
            String result = TelegramMessageUtils.cleanDescription(text);

            // Then
            assertThat(result).doesNotMatch(".*[ \\t]{2,}.*");
            assertThat(result).doesNotContain("\n\n\n");
            assertThat(result).contains("Первый абзац.");
            assertThat(result).contains("Второй абзац.");
        }

        @Test
        void should_truncateAtWordBoundary_when_textExceeds300Chars() {
            // Given — build a long text > 300 chars
            String text = "Очень длинное описание аниме, которое содержит множество деталей о сюжете и персонажах. "
                    + "Здесь описываются приключения главного героя в фэнтези мире, полном магии и опасностей. "
                    + "Каждая серия приносит новые открытия и испытания, которые меняют его судьбу и мировоззрение. "
                    + "Финальная битва решит судьбу всего мира и всех его обитателей навсегда.";

            // When
            String result = TelegramMessageUtils.cleanDescription(text);

            // Then
            assertThat(result.length()).isLessThanOrEqualTo(300);
            assertThat(result).endsWith("...");
        }

        @Test
        void should_notTruncate_when_textIsWithin300Chars() {
            // Given
            String text = "Краткое описание аниме.";

            // When
            String result = TelegramMessageUtils.cleanDescription(text);

            // Then
            assertThat(result).isEqualTo("Краткое описание аниме.");
            assertThat(result).doesNotEndWith("...");
        }
    }
}
