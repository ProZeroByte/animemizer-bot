package pro.zerobyte.animemizerbot.bot.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCallback;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCommand;
import pro.zerobyte.animemizerbot.bot.handler.CommandHandler;
import pro.zerobyte.animemizerbot.core.anime.AnimeListService;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;

import java.util.List;

/**
 * Handles the /watching command — shows anime with status WATCHING, one line per anime
 * with watched/total episode progress and a link that opens the anime card.
 */
@Component
public class WatchingCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(WatchingCommandHandler.class);
    private static final String SEARCH_PLACEHOLDER = "\uD83D\uDD0D Поиск...";
    private static final LinkPreviewOptions DISABLED_LINK_PREVIEW =
            LinkPreviewOptions.builder().isDisabled(true).build();

    private final AnimeListService animeListService;
    private final TelegramClient telegramClient;

    public WatchingCommandHandler(AnimeListService animeListService,
                                  TelegramClient telegramClient) {
        this.animeListService = animeListService;
        this.telegramClient = telegramClient;
    }

    @Override
    public AnimemizerBotCommand getCommand() {
        return AnimemizerBotCommand.WATCHING;
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
            log.error("Failed to send watching list placeholder for chatId={}: {}", chatId, e.getMessage());
            return buildFallbackResult(chatId, telegramUserId);
        }

        // Build watching list and replace placeholder
        List<AnimeListService.WatchingEntry> entries = animeListService.fetchEntries(telegramUserId, WatchStatus.WATCHING);
        String pageText = animeListService.buildStatusPageText(entries, 0, WatchStatus.WATCHING);
        int totalPages = animeListService.totalPages(entries.size());
        InlineKeyboardMarkup keyboard = animeListService.buildStatusPageKeyboard(
                telegramUserId, 0, totalPages, AnimemizerBotCallback.WATCHING_PAGE);

        try {
            EditMessageText.EditMessageTextBuilder<?, ?> builder = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(placeholder.getMessageId())
                    .text(pageText)
                    .parseMode("HTML")
                    .linkPreviewOptions(DISABLED_LINK_PREVIEW);
            if (keyboard != null) {
                builder.replyMarkup(keyboard);
            }
            telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            log.error("Failed to update watching list placeholder for chatId={}: {}", chatId, e.getMessage());
        }

        return null;
    }

    /**
     * Fallback when placeholder send fails — returns result directly for the processor to execute.
     */
    private PartialBotApiMethod<?> buildFallbackResult(long chatId, long telegramUserId) {
        List<AnimeListService.WatchingEntry> entries =
                animeListService.fetchEntries(telegramUserId, WatchStatus.WATCHING);
        String pageText = animeListService.buildStatusPageText(entries, 0, WatchStatus.WATCHING);
        int totalPages = animeListService.totalPages(entries.size());
        InlineKeyboardMarkup keyboard = animeListService.buildStatusPageKeyboard(
                telegramUserId, 0, totalPages, AnimemizerBotCallback.WATCHING_PAGE);

        SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
                .chatId(chatId)
                .text(pageText)
                .parseMode("HTML")
                .linkPreviewOptions(DISABLED_LINK_PREVIEW);
        if (keyboard != null) {
            builder.replyMarkup(keyboard);
        }
        return builder.build();
    }
}
