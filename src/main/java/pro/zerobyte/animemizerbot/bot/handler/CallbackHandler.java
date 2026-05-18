package pro.zerobyte.animemizerbot.bot.handler;

import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface CallbackHandler {

    boolean canHandle(String callbackData);

    PartialBotApiMethod<?> handle(Update update);
}
