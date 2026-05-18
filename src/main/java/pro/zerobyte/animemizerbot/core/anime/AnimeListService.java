package pro.zerobyte.animemizerbot.core.anime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCallback;
import pro.zerobyte.animemizerbot.core.watchlist.Watchlist;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Shared service for anime list pagination logic used by both command and callback handlers.
 */
@Service
public class AnimeListService {

    private static final Logger log = LoggerFactory.getLogger(AnimeListService.class);
    static final int PAGE_SIZE = 20;

    private final WatchlistService watchlistService;
    private final AnimeService animeService;

    public AnimeListService(WatchlistService watchlistService, AnimeService animeService) {
        this.watchlistService = watchlistService;
        this.animeService = animeService;
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

    public int totalPages(List<Anime> animes) {
        if (animes.isEmpty()) {
            return 0;
        }
        return (animes.size() + PAGE_SIZE - 1) / PAGE_SIZE;
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
}

