package pro.zerobyte.animemizerbot.bot.handler.callback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import pro.zerobyte.animemizerbot.bot.message.AnimeMessageBuilder;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;
import pro.zerobyte.animemizerbot.core.watchlist.Watchlist;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchStatusCallbackHandlerTest {

    @Mock
    private AnimeService animeService;

    @Mock
    private WatchlistService watchlistService;

    @Mock
    private AnimeMessageBuilder messageBuilder;

    private static final Long USER_ID = 1L;
    private static final Long CHAT_ID = 12345L;
    private static final int MESSAGE_ID = 100;

    private Update buildCallbackUpdate(String data, Long userId, boolean hasPhoto) {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        User user = mock(User.class);
        Mockito.lenient().when(update.getCallbackQuery()).thenReturn(callbackQuery);
        Mockito.lenient().when(callbackQuery.getData()).thenReturn(data);
        Mockito.lenient().when(callbackQuery.getFrom()).thenReturn(user);
        Mockito.lenient().when(user.getId()).thenReturn(userId);
        Mockito.lenient().when(callbackQuery.getMessage()).thenReturn(message);
        Mockito.lenient().when(message.hasPhoto()).thenReturn(hasPhoto);
        Mockito.lenient().when(message.getChatId()).thenReturn(CHAT_ID);
        Mockito.lenient().when(message.getMessageId()).thenReturn(MESSAGE_ID);
        return update;
    }

    private Anime buildAnime(Long id, String publicId) {
        return new Anime(id, publicId, null, null, "Test Anime", "Тест", null,
                null, null, null, false, false, false, 12, null, null, null, null, null, null);
    }

    private Watchlist buildWatchlist(Long animeId, WatchStatus status, int lastWatchedEpisode) {
        return new Watchlist(1L, USER_ID, animeId, status, lastWatchedEpisode,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("canHandle")
    class CanHandle {

        @Test
        void should_returnTrue_when_setWatchStatusPrefix() {
            // Given
            WatchStatusCallbackHandler handler = new WatchStatusCallbackHandler(
                    animeService, watchlistService, messageBuilder);

            // When
            boolean result = handler.canHandle("SET_WATCH_STATUS:123:WATCHING");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnFalse_when_differentPrefix() {
            // Given
            WatchStatusCallbackHandler handler = new WatchStatusCallbackHandler(
                    animeService, watchlistService, messageBuilder);

            // When
            boolean result = handler.canHandle("NEXT_EPISODE:123");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        void should_setStatusAndReturnEditMessage_when_validCallback() {
            // Given
            WatchStatusCallbackHandler handler = new WatchStatusCallbackHandler(
                    animeService, watchlistService, messageBuilder);
            Update update = buildCallbackUpdate("SET_WATCH_STATUS:42:WATCHING", USER_ID, false);
            Anime anime = buildAnime(42L, "shikimori_42");
            Watchlist watchlist = buildWatchlist(42L, WatchStatus.WATCHING, 0);

            when(animeService.findById(42L)).thenReturn(Optional.of(anime));
            when(watchlistService.setStatus(USER_ID, 42L, WatchStatus.WATCHING)).thenReturn(watchlist);
            when(messageBuilder.buildCaption(anime, 0, WatchStatus.WATCHING)).thenReturn("caption");
            when(messageBuilder.createInlineKeyboard(anime, 0)).thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isInstanceOf(EditMessageText.class);
        }

        @Test
        void should_returnNull_when_invalidFormat() {
            // Given
            WatchStatusCallbackHandler handler = new WatchStatusCallbackHandler(
                    animeService, watchlistService, messageBuilder);
            Update update = buildCallbackUpdate("SET_WATCH_STATUS:42", USER_ID, false);

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
        }

        @Test
        void should_returnNull_when_invalidStatus() {
            // Given
            WatchStatusCallbackHandler handler = new WatchStatusCallbackHandler(
                    animeService, watchlistService, messageBuilder);
            Update update = buildCallbackUpdate("SET_WATCH_STATUS:42:INVALID_STATUS", USER_ID, false);

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
        }

        @Test
        void should_returnNull_when_animeNotFound() {
            // Given
            WatchStatusCallbackHandler handler = new WatchStatusCallbackHandler(
                    animeService, watchlistService, messageBuilder);
            Update update = buildCallbackUpdate("SET_WATCH_STATUS:42:WATCHING", USER_ID, false);

            when(animeService.findById(42L)).thenReturn(Optional.empty());

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
        }
    }
}
