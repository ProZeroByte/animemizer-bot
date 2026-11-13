package pro.zerobyte.animemizerbot.bot.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCommand;
import pro.zerobyte.animemizerbot.bot.handler.CommandHandler;
import pro.zerobyte.animemizerbot.bot.message.AnimeCardSender;
import pro.zerobyte.animemizerbot.core.anime.AnimeSource;
import pro.zerobyte.animemizerbot.core.telegramuser.TelegramUserService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StartCommandHandler implements CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(StartCommandHandler.class);

    /**
     * /start deep-link payload used by anime links in list messages:
     * {@code https://t.me/<bot>?start=show_<shikimoriId>}.
     */
    private static final Pattern SHOW_ANIME_PAYLOAD = Pattern.compile("^show_(\\d{1,15})$");

    private final TelegramUserService telegramUserService;
    private final AnimeCardSender animeCardSender;

    public StartCommandHandler(TelegramUserService telegramUserService,
                                AnimeCardSender animeCardSender) {
        this.telegramUserService = telegramUserService;
        this.animeCardSender = animeCardSender;
    }

    @Override
    public AnimemizerBotCommand getCommand() {
        return AnimemizerBotCommand.START;
    }

    @Override
    public PartialBotApiMethod<?> handle(Update update) {
        log.debug("Is virtual thread? {}", Thread.currentThread().isVirtual());

        Long chatId = update.getMessage().getChatId();
        User user = update.getMessage().getFrom();

        Long shikimoriId = extractShowAnimeId(update.getMessage().getText());
        if (shikimoriId != null) {
            // Deep link from a list message — ensure the user is registered, then open the card
            telegramUserService.findByTelegramId(user.getId())
                    .orElseGet(() -> telegramUserService.createOrUpdate(user, chatId));
            String publicId = AnimeSource.SHIKIMORI.createPublicId(shikimoriId);
            animeCardSender.sendCardByPublicId(chatId, user.getId(), publicId);
            return null; // AnimeCardSender manages its own Telegram responses
        }

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

    /**
     * Extracts the shikimori id from a /start deep-link argument like {@code show_42}.
     * Returns null for plain /start (or any other argument).
     */
    private Long extractShowAnimeId(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String[] parts = text.strip().split("\\s+", 2);
        if (parts.length < 2) {
            return null;
        }

        Matcher matcher = SHOW_ANIME_PAYLOAD.matcher(parts[1].strip());
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException e) {
            log.warn("Invalid show_ deep-link payload: {}", parts[1]);
            return null;
        }
    }
}
