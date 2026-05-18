package pro.zerobyte.animemizerbot.bot.handler.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCommand;
import pro.zerobyte.animemizerbot.bot.handler.CommandHandler;

@Component
public class StopCommandHandler implements CommandHandler {

    @Override
    public AnimemizerBotCommand getCommand() {
        return AnimemizerBotCommand.STOP;
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.getMessage().getChatId();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("До встречи! Возвращайтесь, когда захотите отслеживать больше аниме! 👋")
                .build();
    }
}
