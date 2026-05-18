package pro.zerobyte.animemizerbot.bot.handler;

import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface TextHandler {

    boolean canHandle(String messageText);

    PartialBotApiMethod<?> handle(Update update);
}
