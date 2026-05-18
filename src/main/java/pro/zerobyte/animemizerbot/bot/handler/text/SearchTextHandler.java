package pro.zerobyte.animemizerbot.bot.handler.text;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.bot.handler.TextHandler;
import pro.zerobyte.animemizerbot.bot.message.AnimeMessageBuilder;
import pro.zerobyte.animemizerbot.bot.message.TelegramMessageUtils;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;
import pro.zerobyte.animemizerbot.core.watchlist.Watchlist;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class SearchTextHandler implements TextHandler {
    private static final Logger log = LoggerFactory.getLogger(SearchTextHandler.class);
    private static final String SEARCH_PLACEHOLDER = "\uD83D\uDD0D Поиск...";

    private final AnimeService animeService;
    private final WatchlistService watchlistService;
    private final AnimeMessageBuilder messageBuilder;
    private final TelegramClient telegramClient;
    private final TaskExecutor taskExecutor;

    private record AnimeCard(String caption, InlineKeyboardMarkup replyMarkup, String posterUrl) {}

    public SearchTextHandler(AnimeService animeService,
                             WatchlistService watchlistService,
                             AnimeMessageBuilder messageBuilder,
                             TelegramClient telegramClient,
                             @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.animeService = animeService;
        this.watchlistService = watchlistService;
        this.messageBuilder = messageBuilder;
        this.telegramClient = telegramClient;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public boolean canHandle(String messageText) {
        if (!StringUtils.hasText(messageText)) {
            return false;
        }
        String stripped = messageText.strip();
        if (!stripped.startsWith("/")) {
            return true;
        }
        String firstWord = stripped.split("\\s+", 2)[0];
        String command = firstWord.contains("@") ? firstWord.split("@", 2)[0] : firstWord;
        return "/search".equalsIgnoreCase(command);
    }

    private String extractQuery(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String stripped = text.strip();
        if (stripped.startsWith("/")) {
            String[] parts = stripped.split("\\s+", 2);
            if (parts.length > 1) {
                return parts[1].strip();
            }
            return "";
        }
        return stripped;
    }

    @Override
    public PartialBotApiMethod<?> handle(Update update) {
        Message message = update.getMessage();
        long chatId = message.getChatId();
        long telegramUserId = message.getFrom().getId();
        String query = extractQuery(message.getText());
        if (query.isBlank()) {
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("Пожалуйста, укажите название аниме.\n*Пример:* `Врата Штейна`")
                    .parseMode("Markdown")
                    .build();
        }

        log.debug("Handling text search for chatId={}, query={}", chatId, query);

        // Send placeholder message immediately
        Message placeholder;
        try {
            placeholder = telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(SEARCH_PLACEHOLDER)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send search placeholder for chatId={}: {}", chatId, e.getMessage());
            // Fallback: return result directly without placeholder
            return buildFallbackResult(chatId, telegramUserId, query);
        }

        // Perform search and replace placeholder with result asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                animeService.findByTitle(query)
                        .ifPresentOrElse(
                                anime -> replaceWithAnime(placeholder, chatId, telegramUserId, anime),
                                () -> replaceWithNotFound(placeholder, chatId, query)
                        );
            } catch (RuntimeException e) {
                log.error("Failed to execute anime search for chatId={}, query={}: {}",
                        chatId, query, e.getMessage(), e);
                replaceWithError(placeholder, chatId);
            }
        }, taskExecutor);

        return null; // Handler manages its own Telegram responses
    }

    private AnimeCard prepareAnimeCard(Anime anime, long telegramUserId) {
        Optional<Watchlist> watchlist = watchlistService.getWatchlist(telegramUserId, anime.id());
        int watchedEpisodes = watchlist.map(Watchlist::lastWatchedEpisode).orElse(0);
        WatchStatus watchStatus = watchlist.map(Watchlist::status).orElse(null);
        String caption = messageBuilder.buildCaption(anime, watchedEpisodes, watchStatus);
        InlineKeyboardMarkup replyMarkup = messageBuilder.createInlineKeyboard(anime, watchedEpisodes);
        return new AnimeCard(caption, replyMarkup, anime.posterUrl());
    }

    private void replaceWithAnime(Message placeholder, long chatId, long telegramUserId, Anime anime) {
        AnimeCard card = prepareAnimeCard(anime, telegramUserId);

        if (card.posterUrl() != null) {
            try {
                InputMediaPhoto media = new InputMediaPhoto(card.posterUrl());
                media.setCaption(card.caption());
                media.setParseMode("Markdown");
                telegramClient.execute(EditMessageMedia.builder()
                        .chatId(chatId)
                        .messageId(placeholder.getMessageId())
                        .media(media)
                        .replyMarkup(card.replyMarkup())
                        .build());
                return;
            } catch (TelegramApiException e) {
                log.warn("EditMessageMedia failed for chatId={}, attempting SendPhoto fallback: {}",
                        chatId, e.getMessage());
                try {
                    telegramClient.execute(SendPhoto.builder()
                            .chatId(chatId)
                            .photo(new InputFile(card.posterUrl()))
                            .caption(card.caption())
                            .parseMode("Markdown")
                            .replyMarkup(card.replyMarkup())
                            .build());
                    deleteMessageQuietly(chatId, placeholder.getMessageId());
                    return;
                } catch (TelegramApiException ex) {
                    log.error("SendPhoto fallback failed for chatId={}, falling back to EditMessageText: {}",
                            chatId, ex.getMessage());
                }
            }
        }

        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(placeholder.getMessageId())
                    .text(card.caption())
                    .parseMode("Markdown")
                    .replyMarkup(card.replyMarkup())
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to replace search result with text for chatId={}, animeId={}: {}",
                    chatId, anime.id(), e.getMessage());
        }
    }

    private void replaceWithNotFound(Message placeholder, long chatId, String query) {
        String safeQuery = TelegramMessageUtils.escapeMarkdown(query);
        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(placeholder.getMessageId())
                    .text("К сожалению, я ничего не нашел по запросу: " + safeQuery)
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to update placeholder with not-found message for chatId={}: {}",
                    chatId, e.getMessage());
        }
    }

    private void replaceWithError(Message placeholder, long chatId) {
        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(placeholder.getMessageId())
                    .text("Произошла ошибка при поиске аниме. Пожалуйста, попробуйте позже.")
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to update placeholder with error message for chatId={}: {}",
                    chatId, e.getMessage());
        }
    }

    private void deleteMessageQuietly(long chatId, int messageId) {
        try {
            telegramClient.execute(DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Failed to delete placeholder messageId={} for chatId={}: {}",
                    messageId, chatId, e.getMessage());
        }
    }

    /**
     * Fallback when placeholder send fails — returns result directly for the processor to execute.
     */
    private PartialBotApiMethod<?> buildFallbackResult(long chatId, long telegramUserId, String query) {
        try {
            return animeService.findByTitle(query)
                    .map(anime -> {
                        AnimeCard card = prepareAnimeCard(anime, telegramUserId);
                        if (card.posterUrl() != null) {
                            return SendPhoto.builder()
                                    .chatId(chatId)
                                    .photo(new InputFile(card.posterUrl()))
                                    .caption(card.caption())
                                    .parseMode("Markdown")
                                    .replyMarkup(card.replyMarkup())
                                    .build();
                        }
                        return SendMessage.builder()
                                .chatId(chatId)
                                .text(card.caption())
                                .parseMode("Markdown")
                                .replyMarkup(card.replyMarkup())
                                .build();
                    }).orElseGet(() -> SendMessage.builder()
                            .chatId(chatId)
                            .text("К сожалению, я ничего не нашел по запросу: " + TelegramMessageUtils.escapeMarkdown(query))
                            .parseMode("Markdown")
                            .build());
        } catch (RuntimeException e) {
            log.error("Search failed during fallback for chatId={}, query={}: {}",
                    chatId, query, e.getMessage(), e);
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("Произошла ошибка при поиске аниме. Пожалуйста, попробуйте позже.")
                    .build();
        }
    }
}
