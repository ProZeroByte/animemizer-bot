package pro.zerobyte.animemizerbot.bot.handler.callback;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCallback;
import pro.zerobyte.animemizerbot.bot.handler.CallbackHandler;
import pro.zerobyte.animemizerbot.bot.message.AnimeCardSender;

/**
 * Handles the SHOW_ANIME callback — legacy per-anime buttons from old /watching list
 * messages. New list messages embed deep links instead of buttons; this handler keeps
 * old buttons working by delegating to {@link AnimeCardSender}.
 */
@Component
public class ShowAnimeCallbackHandler implements CallbackHandler {

    private final AnimeCardSender animeCardSender;

    public ShowAnimeCallbackHandler(AnimeCardSender animeCardSender) {
        this.animeCardSender = animeCardSender;
    }

    @Override
    public boolean canHandle(String callbackData) {
        return AnimemizerBotCallback.SHOW_ANIME.isCallbackSupported(callbackData);
    }

    @Override
    public PartialBotApiMethod<?> handle(Update update) {
        Long animeId = AnimemizerBotCallback.SHOW_ANIME.extractId(update.getCallbackQuery().getData());
        if (animeId == null) {
            return null;
        }

        if (!(update.getCallbackQuery().getMessage() instanceof Message sourceMessage)) {
            return null;
        }

        long chatId = sourceMessage.getChatId();
        long telegramUserId = update.getCallbackQuery().getFrom().getId();

        animeCardSender.sendCardByInternalId(chatId, telegramUserId, animeId);
        return null; // AnimeCardSender manages its own Telegram responses
    }
}
