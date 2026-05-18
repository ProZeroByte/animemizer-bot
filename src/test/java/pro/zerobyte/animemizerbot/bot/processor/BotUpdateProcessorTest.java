package pro.zerobyte.animemizerbot.bot.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCommand;
import pro.zerobyte.animemizerbot.bot.handler.CallbackHandler;
import pro.zerobyte.animemizerbot.bot.handler.CommandHandler;
import pro.zerobyte.animemizerbot.bot.handler.TextHandler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotUpdateProcessorTest {

    @Mock
    private TelegramClient telegramClient;

    private Update buildTextUpdate(String text) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        User user = mock(User.class);
        Mockito.lenient().when(update.hasMessage()).thenReturn(true);
        Mockito.lenient().when(update.hasCallbackQuery()).thenReturn(false);
        Mockito.lenient().when(update.getMessage()).thenReturn(message);
        Mockito.lenient().when(message.hasText()).thenReturn(true);
        Mockito.lenient().when(message.getText()).thenReturn(text);
        Mockito.lenient().when(message.getChatId()).thenReturn(12345L);
        Mockito.lenient().when(message.getFrom()).thenReturn(user);
        Mockito.lenient().when(user.getId()).thenReturn(100L);
        return update;
    }

    private Update buildCallbackUpdate(String data) {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        User user = mock(User.class);
        Message message = mock(Message.class);
        Mockito.lenient().when(update.hasCallbackQuery()).thenReturn(true);
        Mockito.lenient().when(update.hasMessage()).thenReturn(true);
        Mockito.lenient().when(update.getCallbackQuery()).thenReturn(callbackQuery);
        Mockito.lenient().when(update.getMessage()).thenReturn(message);
        Mockito.lenient().when(callbackQuery.getData()).thenReturn(data);
        Mockito.lenient().when(callbackQuery.getId()).thenReturn("cb123");
        Mockito.lenient().when(callbackQuery.getFrom()).thenReturn(user);
        Mockito.lenient().when(callbackQuery.getMessage()).thenReturn(message);
        Mockito.lenient().when(user.getId()).thenReturn(100L);
        return update;
    }

    @Nested
    @DisplayName("processUpdate - message routing")
    class MessageRouting {

        @Test
        void should_routeToTextHandler_when_textMessage() {
            // Given
            TextHandler textHandler = mock(TextHandler.class);
            when(textHandler.canHandle("hello")).thenReturn(true);
            when(textHandler.handle(any(Update.class))).thenReturn(null);

            BotUpdateProcessor processor = new BotUpdateProcessor(
                    Collections.emptyList(), Collections.emptyList(), List.of(textHandler), telegramClient);
            Update update = buildTextUpdate("hello");

            // When
            processor.processUpdate(update);

            // Then
            verify(textHandler).canHandle("hello");
            verify(textHandler).handle(update);
        }

        @Test
        void should_routeToCommandHandler_when_commandWithNoTextHandlerMatch() {
            // Given
            TextHandler textHandler = mock(TextHandler.class);
            when(textHandler.canHandle("/start")).thenReturn(false);

            CommandHandler commandHandler = mock(CommandHandler.class);
            when(commandHandler.getCommand()).thenReturn(AnimemizerBotCommand.START);
            when(commandHandler.handle(any(Update.class))).thenReturn(null);

            BotUpdateProcessor processor = new BotUpdateProcessor(
                    List.of(commandHandler), Collections.emptyList(), List.of(textHandler), telegramClient);
            Update update = buildTextUpdate("/start");

            // When
            processor.processUpdate(update);

            // Then
            verify(commandHandler).handle(update);
        }

        @Test
        void should_routeToCallbackHandler_when_callbackQuery() throws TelegramApiException {
            // Given
            when(telegramClient.executeAsync(any(AnswerCallbackQuery.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            CallbackHandler callbackHandler = mock(CallbackHandler.class);
            when(callbackHandler.canHandle("NEXT_EPISODE:123")).thenReturn(true);
            when(callbackHandler.handle(any(Update.class))).thenReturn(null);

            BotUpdateProcessor processor = new BotUpdateProcessor(
                    Collections.emptyList(), List.of(callbackHandler), Collections.emptyList(), telegramClient);
            Update update = buildCallbackUpdate("NEXT_EPISODE:123");

            // When
            processor.processUpdate(update);

            // Then
            verify(callbackHandler).handle(update);
        }

        @Test
        void should_sendUnsupportedMessage_when_noTextOrCallback() throws TelegramApiException {
            // Given
            Update update = mock(Update.class);
            Message message = mock(Message.class);
            when(update.hasCallbackQuery()).thenReturn(false);
            when(update.hasMessage()).thenReturn(true);
            when(update.getMessage()).thenReturn(message);
            when(message.hasText()).thenReturn(false);
            when(message.getChatId()).thenReturn(12345L);

            BotUpdateProcessor processor = new BotUpdateProcessor(
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), telegramClient);

            // When
            processor.processUpdate(update);

            // Then
            verify(telegramClient).execute(any(SendMessage.class));
        }
    }

    @Nested
    @DisplayName("processUpdate - size limits")
    class SizeLimits {

        @Test
        void should_sendError_when_commandExceeds2000Chars() throws TelegramApiException {
            // Given
            String longCommand = "/start " + "a".repeat(2000);
            BotUpdateProcessor processor = new BotUpdateProcessor(
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), telegramClient);
            Update update = buildTextUpdate(longCommand);

            // When
            processor.processUpdate(update);

            // Then
            verify(telegramClient).execute(any(SendMessage.class));
        }

        @Test
        void should_sendError_when_searchTextExceeds255Chars() throws TelegramApiException {
            // Given
            String longText = "a".repeat(256);
            BotUpdateProcessor processor = new BotUpdateProcessor(
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), telegramClient);
            Update update = buildTextUpdate(longText);

            // When
            processor.processUpdate(update);

            // Then
            verify(telegramClient).execute(any(SendMessage.class));
        }

        @Test
        void should_allowNormalLengthMessages() throws TelegramApiException {
            // Given
            TextHandler textHandler = mock(TextHandler.class);
            when(textHandler.canHandle("normal text")).thenReturn(true);
            when(textHandler.handle(any(Update.class))).thenReturn(null);

            BotUpdateProcessor processor = new BotUpdateProcessor(
                    Collections.emptyList(), Collections.emptyList(), List.of(textHandler), telegramClient);
            Update update = buildTextUpdate("normal text");

            // When
            processor.processUpdate(update);

            // Then
            verify(textHandler).handle(update);
            verify(telegramClient, never()).execute(any(SendMessage.class));
        }

        @Test
        void should_notBlockCallback_when_messageTextExceeds255Chars() throws TelegramApiException {
            // Given
            when(telegramClient.executeAsync(any(AnswerCallbackQuery.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            CallbackHandler callbackHandler = mock(CallbackHandler.class);
            when(callbackHandler.canHandle("NEXT_EPISODE:123")).thenReturn(true);
            when(callbackHandler.handle(any(Update.class))).thenReturn(null);

            BotUpdateProcessor processor = new BotUpdateProcessor(
                    Collections.emptyList(), List.of(callbackHandler), Collections.emptyList(), telegramClient);

            Update update = mock(Update.class);
            CallbackQuery callbackQuery = mock(CallbackQuery.class);
            User user = mock(User.class);
            Message message = mock(Message.class);
            Mockito.lenient().when(update.hasCallbackQuery()).thenReturn(true);
            Mockito.lenient().when(update.hasMessage()).thenReturn(true);
            Mockito.lenient().when(update.getCallbackQuery()).thenReturn(callbackQuery);
            Mockito.lenient().when(update.getMessage()).thenReturn(message);
            Mockito.lenient().when(callbackQuery.getData()).thenReturn("NEXT_EPISODE:123");
            Mockito.lenient().when(callbackQuery.getId()).thenReturn("cb123");
            Mockito.lenient().when(callbackQuery.getFrom()).thenReturn(user);
            Mockito.lenient().when(callbackQuery.getMessage()).thenReturn(message);
            Mockito.lenient().when(user.getId()).thenReturn(100L);
            Mockito.lenient().when(message.hasText()).thenReturn(true);
            Mockito.lenient().when(message.getText()).thenReturn("a".repeat(256));

            // When
            processor.processUpdate(update);

            // Then — callback handler must fire, no error message sent
            verify(callbackHandler).handle(update);
            verify(telegramClient, never()).execute(any(SendMessage.class));
        }
    }
}
