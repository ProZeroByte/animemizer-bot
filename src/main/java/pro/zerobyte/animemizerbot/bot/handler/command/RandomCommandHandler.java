package pro.zerobyte.animemizerbot.bot.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCommand;
import pro.zerobyte.animemizerbot.bot.handler.CommandHandler;
import pro.zerobyte.animemizerbot.bot.message.AnimeCardSender;
import pro.zerobyte.animemizerbot.core.telegramuser.TelegramUserService;

/**
 * Handles the /random command — picks a random anime via Shikimori and opens its card.
 */
@Component
public class RandomCommandHandler implements CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(RandomCommandHandler.class);

    private final TelegramUserService telegramUserService;
    private final AnimeCardSender animeCardSender;

    public RandomCommandHandler(TelegramUserService telegramUserService,
                                AnimeCardSender animeCardSender) {
        this.telegramUserService = telegramUserService;
        this.animeCardSender = animeCardSender;
    }

    @Override
    public AnimemizerBotCommand getCommand() {
        return AnimemizerBotCommand.RANDOM;
    }

    @Override
    public PartialBotApiMethod<?> handle(Update update) {
        log.debug("Handling random command. Is virtual thread? {}", Thread.currentThread().isVirtual());

        long chatId = update.getMessage().getChatId();
        User user = update.getMessage().getFrom();

        telegramUserService.findByTelegramId(user.getId())
                .orElseGet(() -> telegramUserService.createOrUpdate(user, chatId));

        animeCardSender.sendRandomCard(chatId, user.getId());
        return null; // AnimeCardSender manages its own Telegram responses
    }
}
