package pro.zerobyte.animemizerbot.bot.handler;

import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface CommandHandler {

    AnimemizerBotCommand getCommand();

    PartialBotApiMethod<?> handle(Update update);
}
