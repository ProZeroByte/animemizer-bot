package pro.zerobyte.animemizerbot.bot.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCommand;
import pro.zerobyte.animemizerbot.bot.handler.CommandHandler;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeListService;

import java.util.List;

@Component
public class ListCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(ListCommandHandler.class);
    private static final String SEARCH_PLACEHOLDER = "\uD83D\uDD0D Поиск...";

    private final AnimeListService animeListService;
    private final TelegramClient telegramClient;

    public ListCommandHandler(AnimeListService animeListService,
                              TelegramClient telegramClient) {
        this.animeListService = animeListService;
        this.telegramClient = telegramClient;
    }

    @Override
    public AnimemizerBotCommand getCommand() {
        return AnimemizerBotCommand.LIST;
    }

    @Override
    public PartialBotApiMethod<?> handle(Update update) {
        final Message message = update.getMessage();
        final long chatId = message.getChatId();
        final long telegramUserId = message.getFrom().getId();

        // Send placeholder immediately
        Message placeholder;
        try {
            placeholder = telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(SEARCH_PLACEHOLDER)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send list placeholder for chatId={}: {}", chatId, e.getMessage());
            return buildFallbackResult(chatId, telegramUserId);
        }

        // Build paginated list and replace placeholder
        List<Anime> sortedAnimes = animeListService.fetchSortedAnimes(telegramUserId);
        String pageText = animeListService.buildPageText(sortedAnimes, 0);
        InlineKeyboardMarkup keyboard = animeListService.buildPageKeyboard(telegramUserId, 0, animeListService.totalPages(sortedAnimes));

        try {
            EditMessageText.EditMessageTextBuilder<?, ?> builder = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(placeholder.getMessageId())
                    .text(pageText);
            if (keyboard != null) {
                builder.replyMarkup(keyboard);
            }
            telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            log.error("Failed to update list placeholder for chatId={}: {}", chatId, e.getMessage());
        }

        return null;
    }

    /**
     * Fallback when placeholder send fails — returns result directly for the processor to execute.
     */
    private PartialBotApiMethod<?> buildFallbackResult(long chatId, long telegramUserId) {
        List<Anime> sortedAnimes = animeListService.fetchSortedAnimes(telegramUserId);
        String pageText = animeListService.buildPageText(sortedAnimes, 0);
        InlineKeyboardMarkup keyboard = animeListService.buildPageKeyboard(telegramUserId, 0, animeListService.totalPages(sortedAnimes));

        SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
                .chatId(chatId)
                .text(pageText);
        if (keyboard != null) {
            builder.replyMarkup(keyboard);
        }
        return builder.build();
    }
}
