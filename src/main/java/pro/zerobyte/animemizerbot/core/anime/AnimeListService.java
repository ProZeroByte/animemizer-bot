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
    private static final int WATCHING_TITLE_MAX_LENGTH = 32;
    private static final int PROGRESS_BAR_LENGTH = 10;

    /**
     * An anime from a user's status list, paired with the watched episode count.
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
     * Fetches anime with the given status only, paired with the watched episode count
     * and sorted alphabetically by Russian name.
     */
    public List<WatchingEntry> fetchEntries(long telegramUserId, WatchStatus status) {
        List<Watchlist> matching = watchlistService.findByTelegramUserId(telegramUserId).stream()
                .filter(entry -> entry.status() == status)
                .toList();
        if (matching.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> watchedByAnimeId = matching.stream()
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
     * Builds the page text for the given status as a two-line block per anime. Titles
     * are bold and link to the anime card when a Shikimori id is available.
     */
    public String buildStatusPageText(List<WatchingEntry> entries, int pageIndex, WatchStatus status) {
        if (entries.isEmpty()) {
            return emptyStatusText(status);
        }

        int fromIndex = pageIndex * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, entries.size());
        List<WatchingEntry> pageItems = entries.subList(fromIndex, toIndex);

        StringBuilder sb = new StringBuilder();
        sb.append(statusIcon(status)).append(" <b>").append(status.getRussianName())
                .append(" (").append(entries.size()).append("):</b>");
        sb.append("\n\n");

        for (WatchingEntry entry : pageItems) {
            Anime anime = entry.anime();
            String title = truncateTitle(anime.russianName() != null ? anime.russianName() : anime.name());
            sb.append("\u25AA ").append(buildTitleLink(anime, title)).append("\n");
            sb.append("  <code>");
            if (anime.episodes() == null || anime.episodes() <= 0) {
                sb.append("\u25CB".repeat(PROGRESS_BAR_LENGTH))
                        .append(" ").append(entry.watchedEpisodes()).append("/?</code>");
            } else {
                sb.append(buildProgressBar(entry.watchedEpisodes(), anime.episodes()))
                        .append(" ").append(entry.watchedEpisodes()).append("/").append(anime.episodes())
                        .append("</code>");
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * Builds the status list keyboard: pagination buttons only. Anime links live in the
     * message text itself (see {@link #buildStatusPageText}).
     */
    public InlineKeyboardMarkup buildStatusPageKeyboard(long telegramUserId, int pageIndex, int totalPages,
                                                        AnimemizerBotCallback pageCallback) {
        if (totalPages <= 1) {
            return null;
        }

        List<InlineKeyboardRow> rows = new ArrayList<>();
        List<InlineKeyboardButton> navButtons = new ArrayList<>();
        if (pageIndex > 0) {
            navButtons.add(InlineKeyboardButton.builder()
                    .text("\u25C0\uFE0F Назад")
                    .callbackData(pageCallback.getPrefix() + telegramUserId + ":" + (pageIndex - 1))
                    .build());
        }
        if (pageIndex < totalPages - 1) {
            navButtons.add(InlineKeyboardButton.builder()
                    .text("Вперёд \u25B6\uFE0F")
                    .callbackData(pageCallback.getPrefix() + telegramUserId + ":" + (pageIndex + 1))
                    .build());
        }
        if (!navButtons.isEmpty()) {
            rows.add(new InlineKeyboardRow(navButtons));
        }

        return new InlineKeyboardMarkup(rows);
    }

    private String statusIcon(WatchStatus status) {
        return switch (status) {
            case WATCHING -> "\uD83D\uDC40";
            case ON_HOLD -> "\u23F8\uFE0F";
            default -> "\uD83D\uDCCB";
        };
    }

    private String emptyStatusText(WatchStatus status) {
        return switch (status) {
            case WATCHING -> "Вы пока ничего не смотрите \uD83D\uDC40";
            case ON_HOLD -> "Вы пока ничего не отложили \u23F8\uFE0F";
            default -> "В этом списке пока ничего нет.";
        };
    }

    /**
     * Renders a bold title as an HTML deep link opening the anime card, or as plain
     * escaped text when the anime has no Shikimori public id.
     */
    private String buildTitleLink(Anime anime, String title) {
        String escapedTitle = TelegramMessageUtils.escapeHtml(title);
        Optional<Long> shikimoriId = AnimeSource.SHIKIMORI.extractIdFromPublicId(anime.publicId());
        if (shikimoriId.isEmpty()) {
            return "<b>" + escapedTitle + "</b>";
        }

        String username = botProperties.username().startsWith("@")
                ? botProperties.username().substring(1)
                : botProperties.username();
        String url = "https://t.me/" + username + "?start=show_" + shikimoriId.get();
        return "<a href=\"%s\"><b>%s</b></a>".formatted(url, escapedTitle);
    }

    private String truncateTitle(String title) {
        int titleLength = title.codePointCount(0, title.length());
        if (titleLength <= WATCHING_TITLE_MAX_LENGTH) {
            return title;
        }

        int prefixEnd = title.offsetByCodePoints(0, WATCHING_TITLE_MAX_LENGTH - 3);
        int wordBoundary = title.lastIndexOf(' ', prefixEnd);
        String prefix = wordBoundary > 0
                ? title.substring(0, wordBoundary)
                : title.substring(0, prefixEnd);
        return prefix.stripTrailing() + "...";
    }

    private String buildProgressBar(int watchedEpisodes, int totalEpisodes) {
        int filledSegments = (int) Math.round((double) watchedEpisodes / totalEpisodes * PROGRESS_BAR_LENGTH);
        filledSegments = Math.clamp(filledSegments, 0, PROGRESS_BAR_LENGTH);
        return "\u25CF".repeat(filledSegments) + "\u25CB".repeat(PROGRESS_BAR_LENGTH - filledSegments);
    }

    private String displayName(Anime anime) {
        return (anime.russianName() != null ? anime.russianName() : anime.name()).toLowerCase();
    }
}
