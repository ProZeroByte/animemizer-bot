package pro.zerobyte.animemizerbot.bot.message;

import org.jspecify.annotations.NonNull;
import java.util.regex.Pattern;

public final class TelegramMessageUtils {

    private static final Pattern SHIKIMORI_LINK_PATTERN = Pattern.compile("\\[(?:character|anime|manga|ranobe|person)[^]]*](.*?)\\[/(?:character|anime|manga|ranobe|person)]");
    private static final Pattern DOUBLE_BRACKETS_PATTERN = Pattern.compile("\\[\\[(.*?)]]");
    private static final Pattern JAPANESE_BRACKETS_PATTERN = Pattern.compile("\\s*\\[[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}\\s]+]");
    private static final Pattern BBCODE_PATTERN = Pattern.compile("\\[/?(b|i|u|s|spoiler)]");

    // Patterns for cleanDescription pipeline
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+|www\\.\\S+");
    private static final Pattern GENERIC_TAG_PATTERN = Pattern.compile("\\[[^]]+]");
    private static final Pattern CJK_PATTERN = Pattern.compile("[\\p{IsHan}\\p{IsKatakana}\\p{IsHiragana}\\p{IsHangul}]");
    private static final Pattern MULTI_WHITESPACE_PATTERN = Pattern.compile("[ \\t]{2,}");
    private static final Pattern MULTI_NEWLINE_PATTERN = Pattern.compile("\\n{3,}");
    private static final int DESCRIPTION_LIMIT = 300;

    private TelegramMessageUtils() {
        throw new UnsupportedOperationException("TelegramMessageUtils is a utility class and cannot be instantiated");
    }

    /**
     * Cleans the text from Shikimori-specific markup.
     * Converts "[character=7407]ÐÐ°Ñ€ÑƒÑ‚Ð¾[/character]" into "ÐÐ°Ñ€ÑƒÑ‚Ð¾".
     * Removes Japanese characters inside brackets [ã†ã¡ã¯ ã‚µã‚¹ã‚±].
     * Removes double brackets [[Ð¥Ð¾ÐºÐ°Ð³Ñ]] -> Ð¥Ð¾ÐºÐ°Ð³Ñ.
     */
    public static @NonNull String cleanShikimoriMarkup(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // 1. Unwrap links to characters, anime, manga, light novels, and people
        String cleaned = SHIKIMORI_LINK_PATTERN.matcher(text).replaceAll("$1");

        // 2. Unwrap double brackets (links to Shikimori dictionary)
        cleaned = DOUBLE_BRACKETS_PATTERN.matcher(cleaned).replaceAll("$1");

        // 3. Remove Japanese names in single brackets
        cleaned = JAPANESE_BRACKETS_PATTERN.matcher(cleaned).replaceAll("");

        // 4. Remove basic BBCode formatting tags
        cleaned = BBCODE_PATTERN.matcher(cleaned).replaceAll("");

        return cleaned;
    }

    /**
     * Escapes Markdown special characters for Telegram Markdown V1.
     * Only escapes characters that are actually parsed as Markdown V1 formatting:
     * _ (italic), * (bold), ` (code), [ ] ( ) (links).
     */
    public static @NonNull String escapeMarkdown(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("`", "\\`");
    }


    /**
     * Escapes HTML special characters for Telegram HTML parse mode.
     * Telegram HTML requires only {@code &}, {@code <} and {@code >} to be escaped in text.
     */
    public static @NonNull String escapeHtml(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replace("&", '&' + "amp;")
                .replace("<", '&' + "lt;")
                .replace(">", '&' + "gt;");
    }

    /**
     * Truncates the text to the specified limit and appends an ellipsis if the text was truncated.
     */
    public static @NonNull String truncateText(String text, int limit) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit - 3) + "...";
    }

    /**
     * Cleans a raw anime description for safe display in Telegram.
     * Pipeline: strip URLs â†’ clean Shikimori markup â†’ strip generic BBCode tags â†’
     * strip CJK characters â†’ normalize whitespace â†’ truncate to 300 chars.
     */
    public static @NonNull String cleanDescription(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // 1. Strip URLs
        String cleaned = URL_PATTERN.matcher(text).replaceAll("");

        // 2. Clean Shikimori markup (character links, double brackets, Japanese names, basic BBCode)
        cleaned = cleanShikimoriMarkup(cleaned);

        // 3. Strip all remaining generic BBCode-style tags [anything]
        cleaned = GENERIC_TAG_PATTERN.matcher(cleaned).replaceAll("");

        // 4. Strip CJK characters (Han, Katakana, Hiragana, Hangul)
        cleaned = CJK_PATTERN.matcher(cleaned).replaceAll("");

        // 5. Normalize whitespace
        cleaned = MULTI_WHITESPACE_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = MULTI_NEWLINE_PATTERN.matcher(cleaned).replaceAll("\n\n");
        cleaned = cleaned.strip();

        // 6. Truncate with word-boundary awareness
        if (cleaned.length() > DESCRIPTION_LIMIT) {
            cleaned = cleaned.substring(0, DESCRIPTION_LIMIT);
            int lastSpace = cleaned.lastIndexOf(' ');
            if (lastSpace > DESCRIPTION_LIMIT / 2) {
                cleaned = cleaned.substring(0, lastSpace) + "...";
            } else {
                cleaned = cleaned + "...";
            }
        }

        return cleaned;
    }
}
