package pro.zerobyte.animemizerbot.bot.handler.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.bot.message.AnimeMessageBuilder;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.watchlist.Watchlist;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchTextHandlerTest {

    private static final Long USER_ID = 100L;
    private static final Long CHAT_ID = 12345L;

    @Mock
    private AnimeService animeService;

    @Mock
    private WatchlistService watchlistService;

    @Mock
    private AnimeMessageBuilder messageBuilder;

    @Mock
    private TelegramClient telegramClient;

    private SearchTextHandler createHandler() {
        return createHandler(Runnable::run);
    }

    private SearchTextHandler createHandler(TaskExecutor taskExecutor) {
        return new SearchTextHandler(animeService, watchlistService, messageBuilder, telegramClient, taskExecutor);
    }

    private Update buildTextUpdate(String text) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        User user = mock(User.class);
        when(update.getMessage()).thenReturn(message);
        when(message.getText()).thenReturn(text);
        when(message.getChatId()).thenReturn(CHAT_ID);
        when(message.getFrom()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);
        return update;
    }

    @Nested
    @DisplayName("canHandle")
    class CanHandle {

        @Test
        void should_returnTrue_when_plainText() {
            // Given
            SearchTextHandler handler = createHandler();

            // When
            boolean result = handler.canHandle("some anime name");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnTrue_when_searchCommand() {
            // Given
            SearchTextHandler handler = createHandler();

            // When
            boolean result = handler.canHandle("/search Naruto");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnTrue_when_bareSearchCommand() {
            // Given
            SearchTextHandler handler = createHandler();

            // When
            boolean result = handler.canHandle("/search");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnTrue_when_searchCommandWithBotMention() {
            // Given
            SearchTextHandler handler = createHandler();

            // When
            boolean result = handler.canHandle("/search@animemizer_bot Death Note");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnTrue_when_bareSearchCommandWithBotMention() {
            // Given
            SearchTextHandler handler = createHandler();

            // When
            boolean result = handler.canHandle("/search@animemizer_bot");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnTrue_when_searchCommandWithMultipleSpaces() {
            // Given
            SearchTextHandler handler = createHandler();

            // When
            boolean result = handler.canHandle("/search   Naruto");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnFalse_when_otherCommand() {
            // Given
            SearchTextHandler handler = createHandler();

            // When
            boolean result = handler.canHandle("/start");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void should_returnFalse_when_otherCommandWithBotMention() {
            // Given
            SearchTextHandler handler = createHandler();

            // When
            boolean result = handler.canHandle("/start@animemizer_bot");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void should_returnFalse_when_blank() {
            // Given
            SearchTextHandler handler = createHandler();

            // When
            boolean result = handler.canHandle("   ");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void should_returnFalse_when_null() {
            // Given
            SearchTextHandler handler = createHandler();

            // When
            boolean result = handler.canHandle(null);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        void should_returnUsageMessage_when_bareSearchCommand() {
            // Given
            SearchTextHandler handler = createHandler();
            Update update = buildTextUpdate("/search");

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage sendMessage = (SendMessage) result;
            assertThat(sendMessage.getText()).contains("Пожалуйста, укажите название аниме");
            org.mockito.Mockito.verifyNoInteractions(animeService);
            org.mockito.Mockito.verifyNoInteractions(telegramClient);
        }

        @Test
        void should_returnUsageMessage_when_bareSearchCommandWithTrailingSpaces() {
            // Given
            SearchTextHandler handler = createHandler();
            Update update = buildTextUpdate("/search   ");

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage sendMessage = (SendMessage) result;
            assertThat(sendMessage.getText()).contains("Пожалуйста, укажите название аниме");
            org.mockito.Mockito.verifyNoInteractions(animeService);
            org.mockito.Mockito.verifyNoInteractions(telegramClient);
        }

        @Test
        void should_stripSearchPrefix_when_searchCommandWithArguments() throws TelegramApiException {
            // Given
            SearchTextHandler handler = createHandler();
            Update update = buildTextUpdate("/search Naruto");

            Message placeholder = mock(Message.class);
            when(placeholder.getMessageId()).thenReturn(123);
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);
            when(animeService.findByTitle("Naruto")).thenReturn(Optional.empty());

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            org.mockito.Mockito.verify(animeService).findByTitle("Naruto");
        }

        @Test
        void should_stripSearchPrefixAndBotMention_when_searchCommandWithBotMention() throws TelegramApiException {
            // Given
            SearchTextHandler handler = createHandler();
            Update update = buildTextUpdate("/search@animemizer_bot Death Note");

            Message placeholder = mock(Message.class);
            when(placeholder.getMessageId()).thenReturn(123);
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);
            when(animeService.findByTitle("Death Note")).thenReturn(Optional.empty());

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            org.mockito.Mockito.verify(animeService).findByTitle("Death Note");
        }

        @Test
        void should_stripSearchPrefix_when_multipleSpacesBetweenCommandAndQuery() throws TelegramApiException {
            // Given
            SearchTextHandler handler = createHandler();
            Update update = buildTextUpdate("/search   Naruto  ");

            Message placeholder = mock(Message.class);
            when(placeholder.getMessageId()).thenReturn(123);
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);
            when(animeService.findByTitle("Naruto")).thenReturn(Optional.empty());

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            org.mockito.Mockito.verify(animeService).findByTitle("Naruto");
        }

        @Test
        void should_sendPlaceholderAndReplaceWithResult_when_animeFound() throws TelegramApiException {
            // Given
            SearchTextHandler handler = createHandler();
            Update update = buildTextUpdate("Naruto");

            Message placeholder = mock(Message.class);
            when(placeholder.getMessageId()).thenReturn(123);
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);

            Anime anime = mock(Anime.class);
            when(animeService.findByTitle("Naruto")).thenReturn(Optional.of(anime));
            when(watchlistService.getWatchlist(anyLong(), any())).thenReturn(Optional.empty());
            when(messageBuilder.buildCaption(any(), any(), any())).thenReturn("caption");
            when(messageBuilder.createInlineKeyboard(any(), any(int.class))).thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
        }

        @Test
        void should_sendPlaceholderAndReplaceWithNotFound_when_noResults() throws TelegramApiException {
            // Given
            SearchTextHandler handler = createHandler();
            Update update = buildTextUpdate("NonExistentAnime");

            Message placeholder = mock(Message.class);
            when(placeholder.getMessageId()).thenReturn(123);
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);
            when(animeService.findByTitle("NonExistentAnime")).thenReturn(Optional.empty());

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
        }

        @Test
        void should_returnDirectResult_when_placeholderSendFails() throws TelegramApiException {
            // Given
            SearchTextHandler handler = createHandler();
            Update update = buildTextUpdate("Naruto");

            when(telegramClient.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("Network error"));

            Anime anime = mock(Anime.class);
            when(anime.posterUrl()).thenReturn("http://poster.url");
            when(animeService.findByTitle("Naruto")).thenReturn(Optional.of(anime));
            when(watchlistService.getWatchlist(anyLong(), any())).thenReturn(Optional.empty());
            when(messageBuilder.buildCaption(any(), any(), any())).thenReturn("caption");
            when(messageBuilder.createInlineKeyboard(any(), any(int.class))).thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        void should_returnDirectResult_when_placeholderSendFailsAndNoAnime() throws TelegramApiException {
            // Given
            SearchTextHandler handler = createHandler();
            Update update = buildTextUpdate("NonExistent");

            when(telegramClient.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("Network error"));
            when(animeService.findByTitle("NonExistent")).thenReturn(Optional.empty());

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(SendMessage.class);
        }

        @Test
        void should_executeSearchAsynchronouslyOnTaskExecutor() throws TelegramApiException {
            // Given
            TaskExecutor mockExecutor = mock(TaskExecutor.class);
            SearchTextHandler handler = createHandler(mockExecutor);
            Update update = buildTextUpdate("Naruto");

            Message placeholder = mock(Message.class);
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(mockExecutor).execute(any(Runnable.class));
        }

        @Test
        void should_sendPlaceholderAndReplaceWithMedia_when_animeHasPoster() throws TelegramApiException {
            // Given
            SearchTextHandler handler = createHandler();
            Update update = buildTextUpdate("Naruto");

            Message placeholder = mock(Message.class);
            when(placeholder.getMessageId()).thenReturn(123);
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);

            Anime anime = mock(Anime.class);
            when(anime.posterUrl()).thenReturn("http://poster.url");
            when(animeService.findByTitle("Naruto")).thenReturn(Optional.of(anime));
            when(watchlistService.getWatchlist(anyLong(), any())).thenReturn(Optional.empty());
            when(messageBuilder.buildCaption(any(), any(), any())).thenReturn("caption");
            when(messageBuilder.createInlineKeyboard(any(), any(int.class))).thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(telegramClient).execute(any(EditMessageMedia.class));
        }

        @Test
        void should_fallbackToSendPhotoAndDeletePlaceholder_when_editMessageMediaFails() throws TelegramApiException {
            // Given
            SearchTextHandler handler = createHandler();
            Update update = buildTextUpdate("Naruto");

            Message placeholder = mock(Message.class);
            when(placeholder.getMessageId()).thenReturn(123);
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);

            Anime anime = mock(Anime.class);
            when(anime.posterUrl()).thenReturn("http://poster.url");
            when(animeService.findByTitle("Naruto")).thenReturn(Optional.of(anime));
            when(watchlistService.getWatchlist(anyLong(), any())).thenReturn(Optional.empty());
            when(messageBuilder.buildCaption(any(), any(), any())).thenReturn("caption");
            when(messageBuilder.createInlineKeyboard(any(), any(int.class))).thenReturn(mock(InlineKeyboardMarkup.class));

            when(telegramClient.execute(any(EditMessageMedia.class))).thenThrow(new TelegramApiException("Media edit failed"));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(telegramClient).execute(any(SendPhoto.class));
            verify(telegramClient).execute(any(DeleteMessage.class));
        }

        @Test
        void should_fallbackToEditMessageText_when_bothMediaEditAndSendPhotoFail() throws TelegramApiException {
            // Given
            SearchTextHandler handler = createHandler();
            Update update = buildTextUpdate("Naruto");

            Message placeholder = mock(Message.class);
            when(placeholder.getMessageId()).thenReturn(123);
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);

            Anime anime = mock(Anime.class);
            when(anime.posterUrl()).thenReturn("http://poster.url");
            when(animeService.findByTitle("Naruto")).thenReturn(Optional.of(anime));
            when(watchlistService.getWatchlist(anyLong(), any())).thenReturn(Optional.empty());
            when(messageBuilder.buildCaption(any(), any(), any())).thenReturn("caption");
            when(messageBuilder.createInlineKeyboard(any(), any(int.class))).thenReturn(mock(InlineKeyboardMarkup.class));

            when(telegramClient.execute(any(EditMessageMedia.class))).thenThrow(new TelegramApiException("Media edit failed"));
            when(telegramClient.execute(any(SendPhoto.class))).thenThrow(new TelegramApiException("Send photo failed"));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(telegramClient).execute(any(EditMessageText.class));
        }

        @Test
        void should_updatePlaceholderWithError_when_animeServiceThrowsException() throws TelegramApiException {
            // Given
            SearchTextHandler handler = createHandler();
            Update update = buildTextUpdate("Naruto");

            Message placeholder = mock(Message.class);
            when(placeholder.getMessageId()).thenReturn(123);
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);
            when(animeService.findByTitle("Naruto")).thenThrow(new RuntimeException("Upstream Shikimori timeout"));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
            verify(telegramClient).execute(captor.capture());
            assertThat(captor.getValue().getText()).contains("Произошла ошибка при поиске аниме");
        }

        @Test
        void should_returnErrorMessageDirectly_when_placeholderFailsAndAnimeServiceThrows() throws TelegramApiException {
            // Given
            SearchTextHandler handler = createHandler();
            Update update = buildTextUpdate("Naruto");

            when(telegramClient.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("Network error"));
            when(animeService.findByTitle("Naruto")).thenThrow(new RuntimeException("Database down"));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage sendMessage = (SendMessage) result;
            assertThat(sendMessage.getText()).contains("Произошла ошибка при поиске аниме");
        }

        @Test
        void should_escapeMarkdownInNotFoundPlaceholder_when_queryHasSpecialCharacters() throws TelegramApiException {
            // Given
            SearchTextHandler handler = createHandler();
            String query = "Steins;Gate_0 [Test] *special*";
            Update update = buildTextUpdate(query);

            Message placeholder = mock(Message.class);
            when(placeholder.getMessageId()).thenReturn(123);
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);
            when(animeService.findByTitle(query)).thenReturn(Optional.empty());

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
            verify(telegramClient).execute(captor.capture());
            assertThat(captor.getValue().getText()).contains("Steins;Gate\\_0 \\[Test\\] \\*special\\*");
        }

        @Test
        void should_escapeMarkdownInFallbackResult_when_placeholderFailsAndQueryHasSpecialCharacters() throws TelegramApiException {
            // Given
            SearchTextHandler handler = createHandler();
            String query = "Steins;Gate_0 [Test] *special*";
            Update update = buildTextUpdate(query);

            when(telegramClient.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("Network error"));
            when(animeService.findByTitle(query)).thenReturn(Optional.empty());

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage sendMessage = (SendMessage) result;
            assertThat(sendMessage.getText()).contains("Steins;Gate\\_0 \\[Test\\] \\*special\\*");
        }
    }
}
