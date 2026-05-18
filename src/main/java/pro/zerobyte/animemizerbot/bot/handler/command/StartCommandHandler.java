package pro.zerobyte.animemizerbot.bot.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCommand;
import pro.zerobyte.animemizerbot.bot.handler.CommandHandler;
import pro.zerobyte.animemizerbot.core.telegramuser.TelegramUserService;

@Component
public class StartCommandHandler implements CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(StartCommandHandler.class);

    private final TelegramUserService telegramUserService;

    public StartCommandHandler(TelegramUserService telegramUserService) {
        this.telegramUserService = telegramUserService;
    }

    @Override
    public AnimemizerBotCommand getCommand() {
        return AnimemizerBotCommand.START;
    }

    @Override
    public SendMessage handle(Update update) {
        log.debug("Is virtual thread? {}", Thread.currentThread().isVirtual());

        Long chatId = update.getMessage().getChatId();
        User user = update.getMessage().getFrom();

        String welcomeMessage = telegramUserService.findByTelegramId(user.getId())
                .map(telegramUser -> """
                        Привет, %s! 🍿
                        
                        Что посмотрим сегодня?
                        
                        ⚙️ /help — Подробное описание всех возможностей
                        """.formatted(telegramUser.firstName()))
                .orElseGet(() -> {
                    telegramUserService.createOrUpdate(user, chatId);
                    return """
                            Добро пожаловать в Animemizer Bot, %s! 🎊\s
                            
                            Здесь вы можете легко управлять своим списком аниме и находить новые тайтлы.
                            
                            🔎 <b>Поиск:</b>
                            Для поиска аниме просто введите его название в чат.
                            
                            ⚙️ <b>Прочее:</b>
                            /help — Подробное описание всех возможностей
                            """.formatted(user.getFirstName());
                });

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(welcomeMessage)
                .parseMode("HTML")
                .build();
    }
}
