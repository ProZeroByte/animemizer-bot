package pro.zerobyte.animemizerbot.bot.handler.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.springframework.stereotype.Component;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCallback;
import pro.zerobyte.animemizerbot.bot.handler.CallbackHandler;
import pro.zerobyte.animemizerbot.bot.message.AnimeMessageBuilder;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;
import pro.zerobyte.animemizerbot.core.watchlist.Watchlist;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;

import java.util.Optional;
import java.util.function.Function;

/**
 * Handles both LOAD_PREQUEL and LOAD_SEQUEL callbacks.
 * Parameterized by the callback enum, public ID extractor, and not-found message.
 */
@Component
public class RelationCallbackHandler implements CallbackHandler {
    private static final Logger log = LoggerFactory.getLogger(RelationCallbackHandler.class);
    private static final String SEARCH_PLACEHOLDER = "\uD83D\uDD0D Поиск...";

    private final AnimeService animeService;
    private final WatchlistService watchlistService;
    private final AnimeMessageBuilder messageBuilder;
    private final TelegramClient telegramClient;

    public RelationCallbackHandler(AnimeService animeService,
                                   WatchlistService watchlistService,
                                   AnimeMessageBuilder messageBuilder,
                                   TelegramClient telegramClient) {
        this.animeService = animeService;
        this.watchlistService = watchlistService;
        this.messageBuilder = messageBuilder;
        this.telegramClient = telegramClient;
    }

    @Override
    public boolean canHandle(String callbackData) {
        return AnimemizerBotCallback.LOAD_PREQUEL.isCallbackSupported(callbackData)
                || AnimemizerBotCallback.LOAD_SEQUEL.isCallbackSupported(callbackData);
    }

    @Override
    public PartialBotApiMethod<?> handle(Update update) {
        String data = update.getCallbackQuery().getData();
        boolean isPrequel = AnimemizerBotCallback.LOAD_PREQUEL.isCallbackSupported(data);

        AnimemizerBotCallback callback = isPrequel ? AnimemizerBotCallback.LOAD_PREQUEL : AnimemizerBotCallback.LOAD_SEQUEL;
        Long animeId = callback.extractId(data);
        if (animeId == null) {
            return null;
        }

        if (!(update.getCallbackQuery().getMessage() instanceof Message message)) {
            return null;
        }

        long chatId = message.getChatId();
        int messageId = message.getMessageId();

        String relationLabel = isPrequel ? "prequel" : "sequel";
        String notFoundText = isPrequel ? "Предыстория не найдена" : "Продолжение не найдено";
        Function<Anime, String> publicIdExtractor = isPrequel ? Anime::prequelPublicId : Anime::sequelPublicId;

        // Show placeholder immediately
        showPlaceholder(message, chatId, messageId, relationLabel);

        // Fetch related anime and replace placeholder with result
        Long telegramUserId = update.getCallbackQuery().getFrom().getId();
        animeService.findById(animeId)
                .map(publicIdExtractor)
                .flatMap(animeService::findByPublicIdOrFetch)
                .ifPresentOrElse(
                        anime -> replaceWithAnime(message, chatId, messageId, telegramUserId, anime, relationLabel),
                        () -> showNotFound(message, chatId, messageId, notFoundText, relationLabel));

        return null;
    }

    private void showPlaceholder(Message message, long chatId, int messageId, String relationLabel) {
        try {
            if (message.hasPhoto()) {
                telegramClient.execute(EditMessageCaption.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .caption(SEARCH_PLACEHOLDER)
                        .build());
            } else {
                telegramClient.execute(EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .text(SEARCH_PLACEHOLDER)
                        .build());
            }
        } catch (TelegramApiException e) {
            log.error("Failed to send {} placeholder for chatId={}", relationLabel, chatId, e);
        }
    }

    private void showNotFound(Message message, long chatId, int messageId, String notFoundText, String relationLabel) {
        try {
            if (message.hasPhoto()) {
                telegramClient.execute(EditMessageCaption.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .caption(notFoundText)
                        .build());
            } else {
                telegramClient.execute(EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .text(notFoundText)
                        .build());
            }
        } catch (TelegramApiException e) {
            log.error("Failed to show 'not found' for {} chatId={}", relationLabel, chatId, e);
        }
    }

    private void replaceWithAnime(Message originalMessage, long chatId, int messageId,
                                  long telegramUserId, Anime anime, String relationLabel) {
        Optional<Watchlist> watchlist = watchlistService.getWatchlist(telegramUserId, anime.id());
        int watchedEpisodes = watchlist.map(Watchlist::lastWatchedEpisode).orElse(0);
        WatchStatus watchStatus = watchlist.map(Watchlist::status).orElse(null);
        String caption = messageBuilder.buildCaption(anime, watchedEpisodes, watchStatus);
        InlineKeyboardMarkup markup = messageBuilder.createInlineKeyboard(anime, watchedEpisodes);

        try {
            String posterUrl = anime.posterUrl();
            if (posterUrl != null && !originalMessage.hasPhoto()) {
                InputMediaPhoto media = new InputMediaPhoto(posterUrl);
                media.setCaption(caption);
                media.setParseMode("Markdown");
                telegramClient.execute(EditMessageMedia.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .media(media)
                        .replyMarkup(markup)
                        .build());
            } else if (originalMessage.hasPhoto()) {
                telegramClient.execute(EditMessageCaption.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .caption(caption)
                        .parseMode("Markdown")
                        .replyMarkup(markup)
                        .build());
            } else {
                telegramClient.execute(EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .text(caption)
                        .parseMode("Markdown")
                        .replyMarkup(markup)
                        .build());
            }
        } catch (TelegramApiException e) {
            log.error("Failed to replace {} result for chatId={}, animeId={}",
                    relationLabel, chatId, anime.id(), e);
        }
    }
}

