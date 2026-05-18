package pro.zerobyte.animemizerbot.bot.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCommand;
import pro.zerobyte.animemizerbot.bot.handler.CommandHandler;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles the /batch command — accepts a comma-separated list of anime titles
 * and searches them one by one (sequential, paced with a delay) to avoid
 * hammering upstream providers and Telegram with bursts of requests.
 * Designed to comfortably handle 100+ titles per invocation.
 */
@Component
public class BatchSearchCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(BatchSearchCommandHandler.class);

    private static final int MAX_TITLES = 300;
    private static final long DELAY_BETWEEN_SEARCHES_MS = 400L;
    private static final int PROGRESS_UPDATE_EVERY = 10;
    private static final int TELEGRAM_MESSAGE_LIMIT = 4096;
    // Leave headroom below the hard Telegram limit for headers/formatting.
    private static final int CHUNK_LIMIT = 3800;

    private final AnimeService animeService;
    private final TelegramClient telegramClient;
    private final TaskExecutor taskExecutor;

    public BatchSearchCommandHandler(AnimeService animeService,
                                     TelegramClient telegramClient,
                                     @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.animeService = animeService;
        this.telegramClient = telegramClient;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public AnimemizerBotCommand getCommand() {
        return AnimemizerBotCommand.BATCH;
    }

    @Override
    public SendMessage handle(Update update) {
        Message message = update.getMessage();
        long chatId = message.getChatId();
        String rawArgs = extractArgs(message.getText());

        List<String> titles = parseTitles(rawArgs);

        if (titles.isEmpty()) {
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("Пожалуйста, укажите список аниме через запятую.\n*Пример:* `/batch Наруто, Блич, Ванпанчмен`")
                    .parseMode("Markdown")
                    .build();
        }

        if (titles.size() > MAX_TITLES) {
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("Слишком много тайтлов за раз: %d. Максимум — %d.".formatted(titles.size(), MAX_TITLES))
                    .build();
        }

        Message placeholder;
        try {
            placeholder = telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(buildProgressText(0, titles.size()))
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send batch placeholder for chatId={}: {}", chatId, e.getMessage());
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("Не удалось запустить пакетный поиск. Попробуйте позже.")
                    .build();
        }

        log.info("Starting batch search for chatId={}, titles={}", chatId, titles.size());

        // Fire-and-forget sequential processing: the handler already returned control to the caller.
        processSequentially(titles, chatId, placeholder.getMessageId())
                .whenComplete((results, throwable) -> {
                    if (throwable != null) {
                        log.error("Batch search failed for chatId={}", chatId, throwable);
                        return;
                    }
                    finishBatch(chatId, placeholder.getMessageId(), results);
                });

        return null; // Handler manages its own Telegram responses.
    }

    private String extractArgs(String text) {
        if (text == null) {
            return "";
        }
        String[] parts = text.split("\\s+", 2);
        return parts.length > 1 ? parts[1] : "";
    }

    private List<String> parseTitles(String rawArgs) {
        if (rawArgs == null || rawArgs.isBlank()) {
            return List.of();
        }
        Set<String> deduped = new LinkedHashSet<>();
        for (String candidate : rawArgs.split(",")) {
            String trimmed = candidate.strip();
            if (!trimmed.isBlank()) {
                deduped.add(trimmed);
            }
        }
        return List.copyOf(deduped);
    }

    /**
     * Processes titles one at a time, spacing each search out by
     * {@link #DELAY_BETWEEN_SEARCHES_MS} via a delayed executor so the upstream
     * providers and Telegram are never hit with a burst of requests, regardless
     * of how many titles are requested.
     */
    private CompletableFuture<List<BatchResult>> processSequentially(List<String> titles, long chatId, int placeholderMessageId) {
        List<BatchResult> results = new ArrayList<>(titles.size());
        return processNext(titles, 0, results, chatId, placeholderMessageId);
    }

    private CompletableFuture<List<BatchResult>> processNext(List<String> titles,
                                                              int index,
                                                              List<BatchResult> results,
                                                              long chatId,
                                                              int placeholderMessageId) {
        if (index >= titles.size()) {
            return CompletableFuture.completedFuture(results);
        }

        String title = titles.get(index);

        return CompletableFuture
                .supplyAsync(() -> searchOne(title), taskExecutor)
                .thenCompose(result -> {
                    results.add(result);
                    maybeUpdateProgress(chatId, placeholderMessageId, results.size(), titles.size());

                    // Space out searches without blocking any thread.
                    CompletableFuture<Void> paced = new CompletableFuture<>();
                    CompletableFuture.delayedExecutor(DELAY_BETWEEN_SEARCHES_MS, TimeUnit.MILLISECONDS, taskExecutor)
                            .execute(() -> paced.complete(null));
                    return paced.thenCompose(_ -> processNext(titles, index + 1, results, chatId, placeholderMessageId));
                });
    }

    private BatchResult searchOne(String title) {
        try {
            Optional<Anime> anime = animeService.findByTitle(title);
            return anime
                    .map(a -> new BatchResult(title, true, Objects.requireNonNullElse(a.russianName(), a.name())))
                    .orElseGet(() -> new BatchResult(title, false, null));
        } catch (RuntimeException e) {
            log.warn("Search failed for title '{}': {}", title, e.getMessage());
            return new BatchResult(title, false, null);
        }
    }

    private void maybeUpdateProgress(long chatId, int placeholderMessageId, int processed, int total) {
        boolean isLast = processed == total;
        if (!isLast && processed % PROGRESS_UPDATE_EVERY != 0) {
            return;
        }
        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(placeholderMessageId)
                    .text(buildProgressText(processed, total))
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Failed to update batch progress for chatId={}: {}", chatId, e.getMessage());
        }
    }

    private String buildProgressText(int processed, int total) {
        return "\uD83D\uDD0D Пакетный поиск: %d / %d...".formatted(processed, total);
    }

    private void finishBatch(long chatId, int placeholderMessageId, List<BatchResult> results) {
        List<String> foundLines = new ArrayList<>();
        List<String> notFoundLines = new ArrayList<>();
        int index = 1;
        for (BatchResult result : results) {
            if (result.found()) {
                foundLines.add(index + ". " + result.resolvedTitle());
            } else {
                notFoundLines.add(index + ". " + result.query());
            }
            index++;
        }

        StringBuilder summary = new StringBuilder();
        summary.append("✅ Готово! Обработано %d тайтлов.\n\n".formatted(results.size()));
        summary.append("Найдено (%d):\n".formatted(foundLines.size()));
        foundLines.forEach(line -> summary.append(line).append('\n'));

        if (!notFoundLines.isEmpty()) {
            summary.append("\n❌ Не найдено (%d):\n".formatted(notFoundLines.size()));
            notFoundLines.forEach(line -> summary.append(line).append('\n'));
        }

        List<String> chunks = chunkText(summary.toString().trim(), CHUNK_LIMIT);
        sendChunks(chatId, placeholderMessageId, chunks);
    }

    private void sendChunks(long chatId, int placeholderMessageId, List<String> chunks) {
        if (chunks.isEmpty()) {
            return;
        }

        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(placeholderMessageId)
                    .text(chunks.getFirst())
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to update batch placeholder with final summary for chatId={}: {}", chatId, e.getMessage());
        }

        for (int i = 1; i < chunks.size(); i++) {
            try {
                telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(chunks.get(i))
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to send batch summary chunk {} for chatId={}: {}", i, chatId, e.getMessage());
            }
        }
    }

    /**
     * Splits text into chunks no larger than {@code limit} characters, breaking
     * on line boundaries where possible so entries are never split mid-line.
     * Guarantees each chunk stays within Telegram's {@value #TELEGRAM_MESSAGE_LIMIT}
     * hard message-length limit.
     */
    private List<String> chunkText(String text, int limit) {
        if (text.isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        String[] lines = text.split("\n");
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (current.length() + line.length() + 1 > limit && !current.isEmpty()) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(line).append('\n');
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    private record BatchResult(String query, boolean found, String resolvedTitle) {}
}

