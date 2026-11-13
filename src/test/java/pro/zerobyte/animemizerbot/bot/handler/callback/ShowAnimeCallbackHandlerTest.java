package pro.zerobyte.animemizerbot.bot.handler.callback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import pro.zerobyte.animemizerbot.bot.message.AnimeCardSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowAnimeCallbackHandlerTest {

    private static final Long CHAT_ID = 12345L;
    private static final Long USER_ID = 100L;

    @Mock
    private AnimeCardSender animeCardSender;

    private Update buildCallbackUpdate(String data) {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        User user = mock(User.class);
        Mockito.lenient().when(update.getCallbackQuery()).thenReturn(callbackQuery);
        Mockito.lenient().when(callbackQuery.getData()).thenReturn(data);
        Mockito.lenient().when(callbackQuery.getFrom()).thenReturn(user);
        Mockito.lenient().when(user.getId()).thenReturn(USER_ID);
        Mockito.lenient().when(callbackQuery.getMessage()).thenReturn(message);
        Mockito.lenient().when(message.getChatId()).thenReturn(CHAT_ID);
        return update;
    }

    @Nested
    @DisplayName("canHandle")
    class CanHandle {

        @Test
        void should_returnTrue_when_showAnimePrefix() {
            // Given
            ShowAnimeCallbackHandler handler = new ShowAnimeCallbackHandler(animeCardSender);

            // When
            boolean result = handler.canHandle("SHOW_ANIME:42");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnFalse_when_differentPrefix() {
            // Given
            ShowAnimeCallbackHandler handler = new ShowAnimeCallbackHandler(animeCardSender);

            // When
            boolean result = handler.canHandle("REFRESH:42");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        void should_delegateToCardSender_when_validCallback() {
            // Given
            ShowAnimeCallbackHandler handler = new ShowAnimeCallbackHandler(animeCardSender);
            Update update = buildCallbackUpdate("SHOW_ANIME:42");

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(animeCardSender).sendCardByInternalId(CHAT_ID, USER_ID, 42L);
        }

        @Test
        void should_doNothing_when_invalidId() {
            // Given
            ShowAnimeCallbackHandler handler = new ShowAnimeCallbackHandler(animeCardSender);
            Update update = buildCallbackUpdate("SHOW_ANIME:abc");

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verifyNoInteractions(animeCardSender);
        }

        @Test
        void should_doNothing_when_messageIsNotAccessible() {
            // Given
            ShowAnimeCallbackHandler handler = new ShowAnimeCallbackHandler(animeCardSender);
            Update update = mock(Update.class);
            CallbackQuery callbackQuery = mock(CallbackQuery.class);
            when(update.getCallbackQuery()).thenReturn(callbackQuery);
            when(callbackQuery.getData()).thenReturn("SHOW_ANIME:42");
            when(callbackQuery.getMessage()).thenReturn(null);

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verifyNoInteractions(animeCardSender);
        }
    }
}
