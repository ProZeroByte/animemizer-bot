package pro.zerobyte.animemizerbot.bot.handler.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCallback;
import pro.zerobyte.animemizerbot.bot.handler.CallbackHandler;
import pro.zerobyte.animemizerbot.bot.message.AnimeMessageBuilder;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;
import pro.zerobyte.animemizerbot.core.watchlist.Watchlist;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Handles the REFRESH callback — force-syncs an ongoing anime with fresh Shikimori data.
 *
 * <p>For non-ongoing anime the button intentionally does nothing: episode counts of
 * finished shows do not change, so a live fetch would be wasted.
 */
@Component
public class RefreshCallbackHandler implements CallbackHandler {
    private static final Logger log = LoggerFactory.getLogger(RefreshCallbackHandler.class);
    private static final String REFRESH_PLACEHOLDER = "\uD83D\uDD04 Обновляю данные...";
    private static final String NOT_FOUND_TEXT = "Аниме не найдено";

    private final AnimeService animeService;
    private final WatchlistService watchlistService;
    private final AnimeMessageBuilder messageBuilder;
    private final TelegramClient telegramClient;
    private final TaskExecutor taskExecutor;

    public RefreshCallbackHandler(AnimeService animeService,
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

    @Override
    public boolean canHandle(String callbackData) {
        return AnimemizerBotCallback.REFRESH.isCallbackSupported(callbackData);
    }

    @Override
    public PartialBotApiMethod<?> handle(Update update) {
        Long animeId = AnimemizerBotCallback.REFRESH.extractId(update.getCallbackQuery().getData());
        if (animeId == null) {
            return null;
        }

        if (!(update.getCallbackQuery().getMessage() instanceof Message message)) {
            return null;
        }

        Optional<Anime> current = animeService.findById(animeId);
        if (current.isEmpty() || !Boolean.TRUE.equals(current.get().isOngoing())) {
            // Non-ongoing anime never change episode counts — the button does nothing.
            return null;
        }

        if (animeService.isRefreshOnCooldown(animeId)) {
            // Already refreshed from Shikimori within the cooldown window (12h) — do nothing.
            log.debug("Refresh for anime {} skipped: cooldown active", animeId);
            return null;
        }

        long chatId = message.getChatId();
        int messageId = message.getMessageId();
        long telegramUserId = update.getCallbackQuery().getFrom().getId();

        showPlaceholder(message, chatId, messageId);

        // The Shikimori fetch blocks (the connector joins its futures internally),
        // so it must not run on the polling/webhook thread.
        CompletableFuture.runAsync(() -> {
            try {
                animeService.refreshById(animeId)
                        .ifPresentOrElse(
                                anime -> renderCard(message, chatId, messageId, telegramUserId, anime),
                                () -> showNotFound(chatId, messageId));
            } catch (RuntimeException e) {
                log.error("Failed to refresh anime {} for chatId={}: {}", animeId, chatId, e.getMessage(), e);
                current.ifPresent(anime -> renderCard(message, chatId, messageId, telegramUserId, anime));
            }
        }, taskExecutor);

        return null; // Handler manages its own Telegram responses
    }

    private void showPlaceholder(Message message, long chatId, int messageId) {
        try {
            if (message.hasPhoto()) {
                telegramClient.execute(EditMessageCaption.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .caption(REFRESH_PLACEHOLDER)
                        .build());
            } else {
                telegramClient.execute(EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .text(REFRESH_PLACEHOLDER)
                        .build());
            }
        } catch (TelegramApiException e) {
            log.error("Failed to show refresh placeholder for chatId={}: {}", chatId, e.getMessage());
        }
    }

    private void showNotFound(long chatId, int messageId) {
        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(NOT_FOUND_TEXT)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to show 'not found' after refresh for chatId={}: {}", chatId, e.getMessage());
        }
    }

    private void renderCard(Message message, long chatId, int messageId, long telegramUserId, Anime anime) {
        Optional<Watchlist> watchlist = watchlistService.getWatchlist(telegramUserId, anime.id());
        int watchedEpisodes = watchlist.map(Watchlist::lastWatchedEpisode).orElse(0);
        WatchStatus watchStatus = watchlist.map(Watchlist::status).orElse(null);
        String caption = messageBuilder.buildCaption(anime, watchedEpisodes, watchStatus);
        InlineKeyboardMarkup markup = messageBuilder.createInlineKeyboard(anime, watchedEpisodes);

        try {
            if (message.hasPhoto()) {
                telegramClient.execute(EditMessageCaption.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .caption(caption)
                        .parseMode("Markdown")
                        .replyMarkup(markup)
                        .build());
            } else {
                telegramClient.execute(EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .text(caption)
                        .parseMode("Markdown")
                        .replyMarkup(markup)
                        .build());
            }
        } catch (TelegramApiException e) {
            log.error("Failed to render refreshed anime card for chatId={}, animeId={}: {}",
                    chatId, anime.id(), e.getMessage());
        }
    }
}
