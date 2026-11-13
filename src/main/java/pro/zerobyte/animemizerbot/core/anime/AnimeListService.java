package pro.zerobyte.animemizerbot.core.anime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCallback;
import pro.zerobyte.animemizerbot.bot.message.TelegramMessageUtils;
import pro.zerobyte.animemizerbot.config.props.TelegramBotProperties;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;
import pro.zerobyte.animemizerbot.core.watchlist.Watchlist;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Shared service for anime list pagination logic used by both command and callback handlers.
 */
@Service
public class AnimeListService {

    private static final Logger log = LoggerFactory.getLogger(AnimeListService.class);
    static final int PAGE_SIZE = 20;

    /**
     * An anime the user is currently watching, paired with the watched episode count.
     */
    public record WatchingEntry(Anime anime, int watchedEpisodes) {}

    private final WatchlistService watchlistService;
    private final AnimeService animeService;
    private final TelegramBotProperties botProperties;

    public AnimeListService(WatchlistService watchlistService,
                            AnimeService animeService,
                            TelegramBotProperties botProperties) {
        this.watchlistService = watchlistService;
        this.animeService = animeService;
        this.botProperties = botProperties;
    }

    /**
     * Fetches all anime in a user's watchlist, sorted alphabetically by Russian name.
     */
    public List<Anime> fetchSortedAnimes(long telegramUserId) {
        List<Watchlist> entries = watchlistService.findByTelegramUserId(telegramUserId);
        if (entries.isEmpty()) {
            return List.of();
        }

        return animeService.findAllById(entries
                        .stream()
                        .map(Watchlist::animeId)
                        .toList())
                .stream()
                .sorted(Comparator.comparing(
                        anime -> anime.russianName() != null
                                ? anime.russianName().toLowerCase()
                                : anime.name().toLowerCase()))
                .toList();
    }

    /**
     * Fetches anime with status WATCHING only, paired with the watched episode count
     * and sorted alphabetically by Russian name.
     */
    public List<WatchingEntry> fetchWatchingEntries(long telegramUserId) {
        List<Watchlist> watching = watchlistService.findByTelegramUserId(telegramUserId).stream()
                .filter(entry -> entry.status() == WatchStatus.WATCHING)
                .toList();
        if (watching.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> watchedByAnimeId = watching.stream()
                .collect(Collectors.toMap(
                        Watchlist::animeId,
                        entry -> Objects.requireNonNullElse(entry.lastWatchedEpisode(), 0),
                        Integer::max));

        return animeService.findAllById(List.copyOf(watchedByAnimeId.keySet()))
                .stream()
                .map(anime -> new WatchingEntry(anime, watchedByAnimeId.getOrDefault(anime.id(), 0)))
                .sorted(Comparator.comparing(entry -> displayName(entry.anime())))
                .toList();
    }

    public int totalPages(List<Anime> animes) {
        return totalPages(animes.size());
    }

    public int totalPages(int itemCount) {
        if (itemCount <= 0) {
            return 0;
        }
        return (itemCount + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    public String buildPageText(List<Anime> sortedAnimes, int pageIndex) {
        if (sortedAnimes.isEmpty()) {
            return "У вас пока нет аниме в списке.";
        }

        int totalPages = totalPages(sortedAnimes);
        int fromIndex = pageIndex * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, sortedAnimes.size());
        List<Anime> pageItems = sortedAnimes.subList(fromIndex, toIndex);

        StringBuilder sb = new StringBuilder();
        if (totalPages > 1) {
            sb.append("\uD83D\uDCD6 Страница ").append(pageIndex + 1).append("/").append(totalPages).append("\n\n");
        }

        int index = fromIndex + 1;
        for (Anime anime : pageItems) {
            String title = anime.russianName() != null ? anime.russianName() : anime.name();
            sb.append(index).append(". ").append(title).append("\n");
            index++;
        }

        return sb.toString().trim();
    }

    public InlineKeyboardMarkup buildPageKeyboard(long telegramUserId, int pageIndex, int totalPages) {
        if (totalPages <= 1) {
            return null;
        }

        List<InlineKeyboardRow> rows = new ArrayList<>();
        List<InlineKeyboardButton> buttons = new ArrayList<>();

        if (pageIndex > 0) {
            buttons.add(InlineKeyboardButton.builder()
                    .text("\u25C0\uFE0F Назад")
                    .callbackData(AnimemizerBotCallback.LIST_PAGE.getPrefix() + telegramUserId + ":" + (pageIndex - 1))
                    .build());
        }

        if (pageIndex < totalPages - 1) {
            buttons.add(InlineKeyboardButton.builder()
                    .text("Вперёд \u25B6\uFE0F")
                    .callbackData(AnimemizerBotCallback.LIST_PAGE.getPrefix() + telegramUserId + ":" + (pageIndex + 1))
                    .build());
        }

        if (!buttons.isEmpty()) {
            rows.add(new InlineKeyboardRow(buttons));
        }

        return new InlineKeyboardMarkup(rows);
    }

    /**
     * Builds the /watching page text: one line per anime with watched/total episode progress.
     * Each title is a Markdown link to the bot ({@code https://t.me/<bot>?start=show_<shikimoriId>}),
     * so the message must be sent with parseMode "Markdown" and link previews disabled.
     */
    public String buildWatchingPageText(List<WatchingEntry> entries, int pageIndex) {
        if (entries.isEmpty()) {
            return "Вы пока ничего не смотрите \uD83D\uDC40";
        }

        int totalPages = totalPages(entries.size());
        int fromIndex = pageIndex * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, entries.size());
        List<WatchingEntry> pageItems = entries.subList(fromIndex, toIndex);

        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDC40 Смотрю (").append(entries.size()).append("):");
        if (totalPages > 1) {
            sb.append("\n\n\uD83D\uDCD6 Страница ").append(pageIndex + 1).append("/").append(totalPages);
        }
        sb.append("\n\n");

        int index = fromIndex + 1;
        for (WatchingEntry entry : pageItems) {
            Anime anime = entry.anime();
            String title = anime.russianName() != null ? anime.russianName() : anime.name();
            String total = (anime.episodes() != null && anime.episodes() > 0)
                    ? String.valueOf(anime.episodes())
                    : "?";
            sb.append(index).append(". ").append(buildTitleLink(anime, title))
                    .append(" — ").append(entry.watchedEpisodes()).append("/").append(total).append("\n");
            index++;
        }

        return sb.toString().trim();
    }

    /**
     * Builds the /watching keyboard: pagination buttons only. Anime links live in the
     * message text itself (see {@link #buildWatchingPageText}).
     */
    public InlineKeyboardMarkup buildWatchingPageKeyboard(long telegramUserId, int pageIndex, int totalPages) {
        if (totalPages <= 1) {
            return null;
        }

        List<InlineKeyboardRow> rows = new ArrayList<>();
        List<InlineKeyboardButton> navButtons = new ArrayList<>();
        if (pageIndex > 0) {
            navButtons.add(InlineKeyboardButton.builder()
                    .text("\u25C0\uFE0F Назад")
                    .callbackData(AnimemizerBotCallback.WATCHING_PAGE.getPrefix() + telegramUserId + ":" + (pageIndex - 1))
                    .build());
        }
        if (pageIndex < totalPages - 1) {
            navButtons.add(InlineKeyboardButton.builder()
                    .text("Вперёд \u25B6\uFE0F")
                    .callbackData(AnimemizerBotCallback.WATCHING_PAGE.getPrefix() + telegramUserId + ":" + (pageIndex + 1))
                    .build());
        }
        if (!navButtons.isEmpty()) {
            rows.add(new InlineKeyboardRow(navButtons));
        }

        return new InlineKeyboardMarkup(rows);
    }

    /**
     * Renders the title as a Markdown deep link opening the anime card, or as plain
     * escaped text when the anime has no Shikimori public id.
     */
    private String buildTitleLink(Anime anime, String title) {
        String escapedTitle = TelegramMessageUtils.escapeMarkdown(title);
        Optional<Long> shikimoriId = AnimeSource.SHIKIMORI.extractIdFromPublicId(anime.publicId());
        if (shikimoriId.isEmpty()) {
            return escapedTitle;
        }

        String username = botProperties.username().startsWith("@")
                ? botProperties.username().substring(1)
                : botProperties.username();
        String url = "https://t.me/" + username + "?start=show_" + shikimoriId.get();
        return "[%s](%s)".formatted(escapedTitle, url);
    }

    private String displayName(Anime anime) {
        return (anime.russianName() != null ? anime.russianName() : anime.name()).toLowerCase();
    }
}

