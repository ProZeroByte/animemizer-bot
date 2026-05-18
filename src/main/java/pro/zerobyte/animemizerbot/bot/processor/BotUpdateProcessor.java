package pro.zerobyte.animemizerbot.bot.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.bot.handler.CallbackHandler;
import pro.zerobyte.animemizerbot.bot.handler.CommandHandler;
import pro.zerobyte.animemizerbot.bot.handler.TextHandler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BotUpdateProcessor {
    private static final Logger log = LoggerFactory.getLogger(BotUpdateProcessor.class);
    private static final int MAX_COMMAND_LENGTH = 2000;
    private static final int MAX_SEARCH_QUERY_LENGTH = 255;

    private final Map<String, CommandHandler> commandHandlers;
    private final List<CallbackHandler> callbackHandlers;
    private final List<TextHandler> textHandlers;
    private final TelegramClient telegramClient;

    public BotUpdateProcessor(List<CommandHandler> commandHandlers,
                              List<CallbackHandler> callbackHandlers,
                              List<TextHandler> textHandlers,
                              TelegramClient telegramClient) {
        this.commandHandlers = commandHandlers.stream().collect(Collectors.toUnmodifiableMap(handler -> handler.getCommand().getCommandName(), Function.identity()));
        this.callbackHandlers = List.copyOf(callbackHandlers);
        this.textHandlers = List.copyOf(textHandlers);
        this.telegramClient = telegramClient;
    }

    public void processUpdate(Update update) {
        if (!update.hasCallbackQuery() && update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            Long userId = message.getFrom().getId();
            String text = message.getText().trim();

            if (text.startsWith("/")) {
                if (text.length() > MAX_COMMAND_LENGTH) {
                    log.warn("Ignored massive command payload from user {}. Length: {}", userId, text.length());
                    execute(SendMessage.builder()
                            .chatId(message.getChatId().toString())
                            .text("Payload too long (max 2000 chars). Please shorten your command.")
                            .build());
                    return;
                }
            } else {
                if (text.length() > MAX_SEARCH_QUERY_LENGTH) {
                    log.warn("Ignored overly long default search query from user {}. Length: {}", userId, text.length());
                    execute(SendMessage.builder()
                            .chatId(message.getChatId().toString())
                            .text("Сообщение слишком длинное (макс. 255 символов). Пожалуйста, сократите текст и попробуйте снова.")
                            .build());
                    return;
                }
            }
        }

        switch (update) {
            case Update u when u.hasCallbackQuery() -> handleCallback(u);
            case Update u when u.hasMessage() && u.getMessage().hasText() -> handleText(u);
            default -> {
                log.info("Unsupported update type: {}", update.getUpdateId());
                if (update.hasMessage()) {
                    Long chatId = update.getMessage().getChatId();
                    execute(SendMessage.builder()
                            .chatId(chatId.toString())
                            .text("Я могу обрабатывать только текстовые сообщения и команды. Введите /help для справки.")
                            .build());
                }
            }
        }
    }

    private void handleCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();
        callbackHandlers
                .stream()
                .filter(h -> h.canHandle(data))
                .findFirst()
                .ifPresentOrElse(handler -> {
                    answerCallbackQuery(callbackQuery.getId());
                    try {
                        PartialBotApiMethod<?> response = handler.handle(update);
                        if (response != null) {
                            execute(response);
                        }
                    } catch (RuntimeException e) {
                        log.error("Callback handler {} failed for data '{}'",
                                handler.getClass().getSimpleName(), data, e);
                    }
                }, () -> {
                    log.warn("Callback handler was not found: {}", data);
                    answerCallbackQuery(callbackQuery.getId());
                });
    }

    private void handleText(Update update) {
        Message message = update.getMessage();
        String text = message.getText();
        Long chatId = message.getChatId();
        textHandlers.stream()
                .filter(h -> h.canHandle(text))
                .findFirst()
                .ifPresentOrElse(
                        handler -> {
                            try {
                                PartialBotApiMethod<?> response = handler.handle(update);
                                if (response != null) {
                                    execute(response);
                                }
                            } catch (RuntimeException e) {
                                log.error("Text handler {} failed for text '{}'",
                                        handler.getClass().getSimpleName(), text, e);
                            }
                        },
                        () -> {
                            try {
                                PartialBotApiMethod<?> response = handleCommand(update, text, chatId);
                                if (response != null) {
                                    execute(response);
                                }
                            } catch (RuntimeException e) {
                                log.error("Command handler failed for text '{}'", text, e);
                            }
                        });

    }

    private PartialBotApiMethod<?> handleCommand(Update update, String text, Long chatId) {
        String rawCommand = text.split("\\s+", 2)[0].toLowerCase();
        String command = rawCommand.contains("@") ? rawCommand.split("@", 2)[0] : rawCommand;
        CommandHandler handler = commandHandlers.get(command);

        if (handler != null) {
            return handler.handle(update);
        }

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Неизвестная команда. Введите /help для списка доступных команд.")
                .build();
    }

    private void answerCallbackQuery(String callbackQueryId) {
        try {
            // Запуск асинхронного вызова
            telegramClient.executeAsync(AnswerCallbackQuery.builder()
                            .callbackQueryId(callbackQueryId)
                            .build())
                    .exceptionally(ex -> {
                        // Обработка ошибок в асинхронном потоке (сеть, сервер Telegram)
                        log.error("Network error answering callback query [ID: {}]", callbackQueryId, ex);
                        return null;
                    });
        } catch (TelegramApiException e) {
            // Обработка синхронных ошибок (ошибка подготовки запроса или валидации внутри библиотеки)
            log.error("Failed to execute answer callback query [ID: {}]", callbackQueryId, e);
        }
    }

    private void execute(PartialBotApiMethod<?> response) {
        try {
            // PartialBotApiMethod is not accepted by TelegramClient.execute(BotApiMethod),
            // so explicit type dispatch is required for each concrete method type.
            switch (response) {
                case SendMessage msg -> telegramClient.execute(msg);
                case SendPhoto photo -> telegramClient.execute(photo);
                case SendVideo video -> telegramClient.execute(video);
                case SendAudio audio -> telegramClient.execute(audio);
                case SendDocument doc -> telegramClient.execute(doc);
                case SendVoice voice -> telegramClient.execute(voice);
                case SendAnimation anim -> telegramClient.execute(anim);
                case EditMessageText edit -> telegramClient.execute(edit);
                case EditMessageCaption caption -> telegramClient.execute(caption);
                case EditMessageMedia media -> telegramClient.execute(media);
                case EditMessageReplyMarkup markup -> telegramClient.execute(markup);
                case DeleteMessage delete -> telegramClient.execute(delete);
                case AnswerCallbackQuery answer -> telegramClient.execute(answer);
                case SetWebhook webhook -> telegramClient.execute(webhook);
                case SetMyCommands commands -> telegramClient.execute(commands);
                default -> log.warn("Unhandled response type: {}", response.getClass().getSimpleName());
            }
        } catch (TelegramApiException e) {
            log.error("Failed to execute response [{}]", response.getClass().getSimpleName(), e);
        }
    }
}
