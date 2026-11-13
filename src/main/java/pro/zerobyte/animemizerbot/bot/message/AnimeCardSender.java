package pro.zerobyte.animemizerbot.bot.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;
import pro.zerobyte.animemizerbot.core.watchlist.Watchlist;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Sends a full anime card (poster, caption, inline keyboard) as a new chat message.
 * Used by flows that "open" an anime from a chat interaction — the SHOW_ANIME callback
 * and /start deep-links embedded in list messages.
 *
 * <p>Follows the placeholder-then-replace pattern: a placeholder message is sent
 * immediately, the anime is fetched asynchronously (the first access may be slow
 * because of watch-link enrichment), then the placeholder is replaced with the card.
 */
@Component
public class AnimeCardSender {
    private static final Logger log = LoggerFactory.getLogger(AnimeCardSender.class);
    private static final String CARD_PLACEHOLDER = "\uD83D\uDD0D Открываю карточку...";
    private static final String NOT_FOUND_TEXT = "Аниме не найдено";
    private static final String ERROR_TEXT = "Произошла ошибка при открытии карточки. Пожалуйста, попробуйте позже.";

    private final AnimeService animeService;
    private final WatchlistService watchlistService;
    private final AnimeMessageBuilder messageBuilder;
    private final TelegramClient telegramClient;
    private final TaskExecutor taskExecutor;

    public AnimeCardSender(AnimeService animeService,
                           WatchlistService watchlistService,
                           AnimeMessageBuilder messageBuilder,
                           TelegramClient telegramClient,
                           @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.animeService = animeService;
        this.watchlistService = watchlistService;
        this.messageBuilder = messageBuilder;
        this.telegramClient = telegramClient;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Opens the card for an anime by its internal database id.
     */
    public void sendCardByInternalId(long chatId, long telegramUserId, Long animeId) {
        sendCard(chatId, telegramUserId, () -> animeService.findById(animeId));
    }

    /**
     * Opens the card for an anime by its public id (e.g. {@code shikimori_42}),
     * fetching fresh data from Shikimori when the record is not in the database.
     */
    public void sendCardByPublicId(long chatId, long telegramUserId, String publicId) {
        sendCard(chatId, telegramUserId, () -> animeService.findByPublicIdOrFetch(publicId));
    }

    private void sendCard(long chatId, long telegramUserId, Supplier<Optional<Anime>> fetch) {
        // Send placeholder message immediately
        Message placeholder = null;
        try {
            placeholder = telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(CARD_PLACEHOLDER)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send card placeholder for chatId={}: {}", chatId, e.getMessage());
            // Fallback: the card is sent directly below
        }

        Message finalPlaceholder = placeholder;

        CompletableFuture.runAsync(() -> {
            try {
                fetch.get().ifPresentOrElse(
                        anime -> renderCard(finalPlaceholder, chatId, telegramUserId, anime),
                        () -> replacePlaceholder(finalPlaceholder, chatId, NOT_FOUND_TEXT, null));
            } catch (RuntimeException e) {
                log.error("Failed to open anime card for chatId={}: {}", chatId, e.getMessage(), e);
                replacePlaceholder(finalPlaceholder, chatId, ERROR_TEXT, null);
            }
        }, taskExecutor);
    }

    private void renderCard(Message placeholder, long chatId, long telegramUserId, Anime anime) {
        Optional<Watchlist> watchlist = watchlistService.getWatchlist(telegramUserId, anime.id());
        int watchedEpisodes = watchlist.map(Watchlist::lastWatchedEpisode).orElse(0);
        WatchStatus watchStatus = watchlist.map(Watchlist::status).orElse(null);
        String caption = messageBuilder.buildCaption(anime, watchedEpisodes, watchStatus);
        InlineKeyboardMarkup markup = messageBuilder.createInlineKeyboard(anime, watchedEpisodes);

        String posterUrl = anime.posterUrl();
        if (posterUrl != null && !posterUrl.isBlank()) {
            try {
                telegramClient.execute(SendPhoto.builder()
                        .chatId(chatId)
                        .photo(new InputFile(posterUrl))
                        .caption(caption)
                        .parseMode("Markdown")
                        .replyMarkup(markup)
                        .build());
                deleteMessageQuietly(chatId, placeholder);
                return;
            } catch (TelegramApiException e) {
                log.warn("SendPhoto failed for chatId={}, falling back to text card: {}", chatId, e.getMessage());
            }
        }

        replacePlaceholder(placeholder, chatId, caption, markup);
    }

    private void replacePlaceholder(Message placeholder, long chatId, String text, InlineKeyboardMarkup markup) {
        try {
            if (placeholder != null) {
                EditMessageText.EditMessageTextBuilder<?, ?> builder = EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(placeholder.getMessageId())
                        .text(text)
                        .parseMode("Markdown");
                if (markup != null) {
                    builder.replyMarkup(markup);
                }
                telegramClient.execute(builder.build());
            } else {
                telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(text)
                        .build());
            }
        } catch (TelegramApiException e) {
            log.error("Failed to replace card placeholder with text for chatId={}: {}", chatId, e.getMessage());
        }
    }

    private void deleteMessageQuietly(long chatId, Message placeholder) {
        if (placeholder == null) {
            return;
        }
        try {
            telegramClient.execute(DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(placeholder.getMessageId())
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Failed to delete card placeholder messageId={} for chatId={}: {}",
                    placeholder.getMessageId(), chatId, e.getMessage());
        }
    }
}
