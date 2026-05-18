package pro.zerobyte.animemizerbot.bot.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCommand;
import pro.zerobyte.animemizerbot.bot.handler.CommandHandler;
import pro.zerobyte.animemizerbot.core.anime.Anime;

import java.util.List;

@Component
public class HelpCommandHandler implements CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(HelpCommandHandler.class);

    @Override
    public AnimemizerBotCommand getCommand() {
        return AnimemizerBotCommand.HELP;
    }

    @Override
    public SendMessage handle(Update update) {
        log.debug("Handling help command. Is virtual thread? {}", Thread.currentThread().isVirtual());

        Long chatId = update.getMessage().getChatId();

        StringBuilder sb = new StringBuilder();
        sb.append("Доступные команды:\n\n");
        for (AnimemizerBotCommand cmd : AnimemizerBotCommand.values()) {
            sb.append(cmd.getCommandName()).append(" — ").append(cmd.getDescription()).append("\n");
        }

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(sb.toString())
                .build();
    }
}

