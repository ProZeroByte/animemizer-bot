package pro.zerobyte.animemizerbot.bot.handler.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCallback;
import pro.zerobyte.animemizerbot.bot.handler.CallbackHandler;
import pro.zerobyte.animemizerbot.core.anime.AnimeListService;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;

import java.util.List;

/**
 * Handles the PLANNED_PAGE callback — pagination within the /planned list.
 * Payload is "telegramUserId:pageIndex"; only the list owner may flip pages.
 */
@Component
public class PlannedPageCallbackHandler implements CallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(PlannedPageCallbackHandler.class);
    private static final String SEARCH_PLACEHOLDER_FORMAT = "\uD83D\uDD0D Поиск страница %d...";
    private static final LinkPreviewOptions DISABLED_LINK_PREVIEW =
            LinkPreviewOptions.builder().isDisabled(true).build();

    private final AnimeListService animeListService;
    private final TelegramClient telegramClient;

    public PlannedPageCallbackHandler(AnimeListService animeListService,
                                      TelegramClient telegramClient) {
        this.animeListService = animeListService;
        this.telegramClient = telegramClient;
    }

    @Override
    public boolean canHandle(String callbackData) {
        return AnimemizerBotCallback.PLANNED_PAGE.isCallbackSupported(callbackData);
    }

    @Override
    public PartialBotApiMethod<?> handle(Update update) {
        String data = update.getCallbackQuery().getData();
        Long clickerId = update.getCallbackQuery().getFrom().getId();

        AnimemizerBotCallback.Payload payload = AnimemizerBotCallback.parse(data).orElse(null);
        if (payload == null) {
            return null;
        }

        // Payload id is "telegramUserId:pageIndex"
        String[] parts = payload.id().split(":");
        if (parts.length != 2) {
            return null;
        }

        long ownerId;
        int pageIndex;
        try {
            ownerId = Long.parseLong(parts[0]);
            pageIndex = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        if (!clickerId.equals(ownerId)) {
            return null;
        }

        if (!(update.getCallbackQuery().getMessage() instanceof Message message)) {
            return null;
        }

        long chatId = message.getChatId();
        int messageId = message.getMessageId();

        // Show placeholder immediately
        showPlaceholder(chatId, messageId, pageIndex + 1);

        // Fetch planned list and replace placeholder with result
        List<AnimeListService.WatchingEntry> entries = animeListService.fetchEntries(ownerId, WatchStatus.PLANNED);
        int totalPages = animeListService.totalPages(entries.size());
        if (totalPages == 0) {
            replaceWithPage(chatId, messageId,
                    animeListService.buildStatusPageText(entries, 0, WatchStatus.PLANNED), null);
            return null;
        }

        pageIndex = Math.clamp(pageIndex, 0, totalPages - 1);

        String pageText = animeListService.buildStatusPageText(entries, pageIndex, WatchStatus.PLANNED);
        InlineKeyboardMarkup keyboard = animeListService.buildStatusPageKeyboard(
                ownerId, pageIndex, totalPages, AnimemizerBotCallback.PLANNED_PAGE);
        replaceWithPage(chatId, messageId, pageText, keyboard);

        return null;
    }

    private void showPlaceholder(long chatId, int messageId, int displayPage) {
        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(SEARCH_PLACEHOLDER_FORMAT.formatted(displayPage))
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to show planned page placeholder for chatId={}", chatId, e);
        }
    }

    private void replaceWithPage(long chatId, int messageId, String text, InlineKeyboardMarkup keyboard) {
        try {
            EditMessageText.EditMessageTextBuilder<?, ?> builder = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(text)
                    .parseMode("HTML")
                    .linkPreviewOptions(DISABLED_LINK_PREVIEW);
            if (keyboard != null) {
                builder.replyMarkup(keyboard);
            }
            telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            log.error("Failed to replace planned page placeholder for chatId={}", chatId, e);
        }
    }
}
