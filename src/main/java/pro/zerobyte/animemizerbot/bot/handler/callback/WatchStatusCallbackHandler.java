package pro.zerobyte.animemizerbot.bot.handler.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCallback;
import pro.zerobyte.animemizerbot.bot.handler.CallbackHandler;
import pro.zerobyte.animemizerbot.bot.message.AnimeMessageBuilder;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;
import pro.zerobyte.animemizerbot.core.watchlist.Watchlist;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;

@Component
public class WatchStatusCallbackHandler implements CallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(WatchStatusCallbackHandler.class);
    private final AnimeService animeService;
    private final WatchlistService watchlistService;
    private final AnimeMessageBuilder messageBuilder;

    public WatchStatusCallbackHandler(AnimeService animeService, WatchlistService watchlistService, AnimeMessageBuilder messageBuilder) {
        this.animeService = animeService;
        this.watchlistService = watchlistService;
        this.messageBuilder = messageBuilder;
    }

    @Override
    public boolean canHandle(String callbackData) {
        return AnimemizerBotCallback.SET_WATCH_STATUS.isCallbackSupported(callbackData);
    }

    @Override
    public PartialBotApiMethod<?> handle(Update update) {
        String data = update.getCallbackQuery().getData();

        // Format: SET_WATCH_STATUS:animeId:statusName
        String payload = data.substring(AnimemizerBotCallback.SET_WATCH_STATUS.getPrefix().length());
        int separatorIndex = payload.indexOf(':');
        if (separatorIndex == -1) {
            log.warn("Invalid SET_WATCH_STATUS callback data format: {}", data);
            return null;
        }

        Long animeId;
        try {
            animeId = Long.parseLong(payload, 0, separatorIndex, 10);
        } catch (NumberFormatException e) {
            log.warn("Invalid animeId in SET_WATCH_STATUS callback: {}", data);
            return null;
        }

        String statusName = payload.substring(separatorIndex + 1);

        WatchStatus watchStatus;
        try {
            watchStatus = WatchStatus.valueOf(statusName);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid WatchStatus '{}' in SET_WATCH_STATUS callback: {}", statusName, data);
            return null;
        }

        Long telegramUserId = update.getCallbackQuery().getFrom().getId();

        return animeService.findById(animeId)
                .map(anime -> {
                    Watchlist watchlist = watchlistService.setStatus(telegramUserId, animeId, watchStatus);

                    String caption = messageBuilder.buildCaption(anime, watchlist.lastWatchedEpisode(), watchlist.status());
                    InlineKeyboardMarkup markup = messageBuilder.createInlineKeyboard(anime, watchlist.lastWatchedEpisode());

                    if (update.getCallbackQuery().getMessage() instanceof Message message) {
                        if (message.hasPhoto()) {
                            return EditMessageCaption.builder()
                                    .chatId(message.getChatId())
                                    .messageId(message.getMessageId())
                                    .caption(caption)
                                    .parseMode("Markdown")
                                    .replyMarkup(markup)
                                    .build();
                        }
                        return EditMessageText.builder()
                                .chatId(message.getChatId())
                                .messageId(message.getMessageId())
                                .text(caption)
                                .parseMode("Markdown")
                                .replyMarkup(markup)
                                .build();
                    }
                    return null;
                })
                .orElse(null);
    }
}

