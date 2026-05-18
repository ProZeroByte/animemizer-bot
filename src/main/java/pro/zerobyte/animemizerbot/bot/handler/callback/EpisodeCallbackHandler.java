package pro.zerobyte.animemizerbot.bot.handler.callback;

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
import pro.zerobyte.animemizerbot.core.watchlist.Watchlist;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;

/**
 * Handles both NEXT_EPISODE and PREVIOUS_EPISODE callbacks.
 * Parameterized by the callback enum and the episode mutation operation.
 */
@Component
public class EpisodeCallbackHandler implements CallbackHandler {

    private final AnimeService animeService;
    private final WatchlistService watchlistService;
    private final AnimeMessageBuilder messageBuilder;

    public EpisodeCallbackHandler(AnimeService animeService, WatchlistService watchlistService, AnimeMessageBuilder messageBuilder) {
        this.animeService = animeService;
        this.watchlistService = watchlistService;
        this.messageBuilder = messageBuilder;
    }

    @Override
    public boolean canHandle(String callbackData) {
        return AnimemizerBotCallback.NEXT_EPISODE.isCallbackSupported(callbackData)
                || AnimemizerBotCallback.PREVIOUS_EPISODE.isCallbackSupported(callbackData);
    }

    @Override
    public PartialBotApiMethod<?> handle(Update update) {
        String data = update.getCallbackQuery().getData();
        boolean isNext = AnimemizerBotCallback.NEXT_EPISODE.isCallbackSupported(data);

        AnimemizerBotCallback callback = isNext ? AnimemizerBotCallback.NEXT_EPISODE : AnimemizerBotCallback.PREVIOUS_EPISODE;
        Long animeId = callback.extractId(data);
        if (animeId == null) {
            return null;
        }

        return animeService.findById(animeId)
                .map(anime -> {
                    Long telegramUserId = update.getCallbackQuery().getFrom().getId();
                    Watchlist watchlist = isNext
                            ? watchlistService.incrementEpisode(telegramUserId, animeId, anime.episodes() != null ? anime.episodes() : 0)
                            : watchlistService.decreaseEpisode(telegramUserId, animeId);

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

