package pro.zerobyte.animemizerbot.bot.handler.callback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.bot.message.AnimeMessageBuilder;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;
import pro.zerobyte.animemizerbot.core.watchlist.Watchlist;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshCallbackHandlerTest {

    @Mock
    private AnimeService animeService;

    @Mock
    private WatchlistService watchlistService;

    @Mock
    private AnimeMessageBuilder messageBuilder;

    @Mock
    private TelegramClient telegramClient;

    private static final Long USER_ID = 1L;
    private static final Long CHAT_ID = 12345L;
    private static final int MESSAGE_ID = 100;

    private RefreshCallbackHandler createHandler() {
        return new RefreshCallbackHandler(animeService, watchlistService, messageBuilder,
                telegramClient, Runnable::run);
    }

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

    private Anime buildOngoingAnime(Long id) {
        return new Anime(id, "shikimori_" + id, null, null, "Test Anime", "Тест", null,
                null, null, null, false, false, true, 12, 8, null, null, null, null, null);
    }

    private Anime buildReleasedAnime(Long id) {
        return new Anime(id, "shikimori_" + id, null, null, "Test Anime", "Тест", null,
                null, null, null, true, false, false, 12, 12, null, null, null, null, null);
    }

    private Watchlist buildWatchlist(Long animeId, WatchStatus status, int lastWatchedEpisode) {
        return new Watchlist(1L, USER_ID, animeId, status, lastWatchedEpisode,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("canHandle")
    class CanHandle {

        @Test
        void should_returnTrue_when_refreshPrefix() {
            // Given
            RefreshCallbackHandler handler = createHandler();

            // When
            boolean result = handler.canHandle("REFRESH:123");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnFalse_when_differentPrefix() {
            // Given
            RefreshCallbackHandler handler = createHandler();

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
        void should_showPlaceholderAndRenderRefreshedCard_when_animeIsOngoing() throws Exception {
            // Given
            RefreshCallbackHandler handler = createHandler();
            Update update = buildCallbackUpdate("REFRESH:42", USER_ID, false);
            Anime ongoingAnime = buildOngoingAnime(42L);
            Anime refreshedAnime = buildOngoingAnime(42L);
            Watchlist watchlist = buildWatchlist(42L, WatchStatus.WATCHING, 5);

            when(animeService.findById(42L)).thenReturn(Optional.of(ongoingAnime));
            when(animeService.refreshById(42L)).thenReturn(Optional.of(refreshedAnime));
            when(watchlistService.getWatchlist(USER_ID, 42L)).thenReturn(Optional.of(watchlist));
            when(messageBuilder.buildCaption(refreshedAnime, 5, WatchStatus.WATCHING)).thenReturn("refreshed caption");
            when(messageBuilder.createInlineKeyboard(refreshedAnime, 5)).thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
            verify(telegramClient, times(2)).execute(captor.capture());
            List<EditMessageText> edits = captor.getAllValues();
            assertThat(edits.get(0).getText()).contains("Обновляю данные");
            assertThat(edits.get(1).getText()).isEqualTo("refreshed caption");
        }

        @Test
        void should_editCaption_when_messageHasPhoto() throws Exception {
            // Given
            RefreshCallbackHandler handler = createHandler();
            Update update = buildCallbackUpdate("REFRESH:42", USER_ID, true);
            Anime ongoingAnime = buildOngoingAnime(42L);
            Anime refreshedAnime = buildOngoingAnime(42L);

            when(animeService.findById(42L)).thenReturn(Optional.of(ongoingAnime));
            when(animeService.refreshById(42L)).thenReturn(Optional.of(refreshedAnime));
            when(watchlistService.getWatchlist(USER_ID, 42L)).thenReturn(Optional.empty());
            when(messageBuilder.buildCaption(refreshedAnime, 0, null)).thenReturn("refreshed caption");
            when(messageBuilder.createInlineKeyboard(refreshedAnime, 0)).thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            ArgumentCaptor<EditMessageCaption> captor = ArgumentCaptor.forClass(EditMessageCaption.class);
            verify(telegramClient, times(2)).execute(captor.capture());
            List<EditMessageCaption> edits = captor.getAllValues();
            assertThat(edits.get(0).getCaption()).contains("Обновляю данные");
            assertThat(edits.get(1).getCaption()).isEqualTo("refreshed caption");
        }

        @Test
        void should_doNothing_when_animeIsNotOngoing() {
            // Given
            RefreshCallbackHandler handler = createHandler();
            Update update = buildCallbackUpdate("REFRESH:42", USER_ID, false);
            Anime releasedAnime = buildReleasedAnime(42L);

            when(animeService.findById(42L)).thenReturn(Optional.of(releasedAnime));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(animeService, never()).refreshById(42L);
            verifyNoInteractions(telegramClient);
        }

        @Test
        void should_doNothing_when_animeNotFound() {
            // Given
            RefreshCallbackHandler handler = createHandler();
            Update update = buildCallbackUpdate("REFRESH:42", USER_ID, false);

            when(animeService.findById(42L)).thenReturn(Optional.empty());

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(animeService, never()).refreshById(42L);
            verifyNoInteractions(telegramClient);
        }

        @Test
        void should_doNothing_when_refreshOnCooldown() {
            // Given
            RefreshCallbackHandler handler = createHandler();
            Update update = buildCallbackUpdate("REFRESH:42", USER_ID, false);
            Anime ongoingAnime = buildOngoingAnime(42L);

            when(animeService.findById(42L)).thenReturn(Optional.of(ongoingAnime));
            when(animeService.isRefreshOnCooldown(42L)).thenReturn(true);

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(animeService, never()).refreshById(42L);
            verifyNoInteractions(telegramClient);
        }

        @Test
        void should_doNothing_when_invalidId() {
            // Given
            RefreshCallbackHandler handler = createHandler();
            Update update = buildCallbackUpdate("REFRESH:abc", USER_ID, false);

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verifyNoInteractions(animeService);
            verifyNoInteractions(telegramClient);
        }

        @Test
        void should_doNothing_when_messageIsNotAccessible() {
            // Given
            RefreshCallbackHandler handler = createHandler();
            Update update = mock(Update.class);
            CallbackQuery callbackQuery = mock(CallbackQuery.class);
            when(update.getCallbackQuery()).thenReturn(callbackQuery);
            when(callbackQuery.getData()).thenReturn("REFRESH:42");
            when(callbackQuery.getMessage()).thenReturn(null);

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verifyNoInteractions(animeService);
            verifyNoInteractions(telegramClient);
        }

        @Test
        void should_renderDatabaseCopy_when_shikimoriFails() throws Exception {
            // Given
            RefreshCallbackHandler handler = createHandler();
            Update update = buildCallbackUpdate("REFRESH:42", USER_ID, false);
            Anime ongoingAnime = buildOngoingAnime(42L);

            when(animeService.findById(42L)).thenReturn(Optional.of(ongoingAnime));
            // AnimeService returns the unchanged database copy when Shikimori is unreachable
            when(animeService.refreshById(42L)).thenReturn(Optional.of(ongoingAnime));
            when(watchlistService.getWatchlist(USER_ID, 42L)).thenReturn(Optional.empty());
            when(messageBuilder.buildCaption(ongoingAnime, 0, null)).thenReturn("current caption");
            when(messageBuilder.createInlineKeyboard(ongoingAnime, 0)).thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
            verify(telegramClient, times(2)).execute(captor.capture());
            List<EditMessageText> edits = captor.getAllValues();
            assertThat(edits.get(1).getText()).isEqualTo("current caption");
        }

        @Test
        void should_showNotFound_when_refreshReturnsEmpty() throws Exception {
            // Given
            RefreshCallbackHandler handler = createHandler();
            Update update = buildCallbackUpdate("REFRESH:42", USER_ID, false);
            Anime ongoingAnime = buildOngoingAnime(42L);

            when(animeService.findById(42L)).thenReturn(Optional.of(ongoingAnime));
            when(animeService.refreshById(42L)).thenReturn(Optional.empty());

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
            verify(telegramClient, times(2)).execute(captor.capture());
            List<EditMessageText> edits = captor.getAllValues();
            assertThat(edits.get(1).getText()).isEqualTo("Аниме не найдено");
        }

        @Test
        void should_renderCurrentCard_when_refreshThrows() throws Exception {
            // Given
            RefreshCallbackHandler handler = createHandler();
            Update update = buildCallbackUpdate("REFRESH:42", USER_ID, false);
            Anime ongoingAnime = buildOngoingAnime(42L);

            when(animeService.findById(42L)).thenReturn(Optional.of(ongoingAnime));
            when(animeService.refreshById(42L)).thenThrow(new RuntimeException("Shikimori timeout"));
            when(watchlistService.getWatchlist(USER_ID, 42L)).thenReturn(Optional.empty());
            when(messageBuilder.buildCaption(ongoingAnime, 0, null)).thenReturn("current caption");
            when(messageBuilder.createInlineKeyboard(ongoingAnime, 0)).thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
            verify(telegramClient, times(2)).execute(captor.capture());
            List<EditMessageText> edits = captor.getAllValues();
            assertThat(edits.get(1).getText()).isEqualTo("current caption");
        }

        @Test
        void should_stillRenderCard_when_placeholderEditFails() throws Exception {
            // Given
            RefreshCallbackHandler handler = createHandler();
            Update update = buildCallbackUpdate("REFRESH:42", USER_ID, false);
            Anime ongoingAnime = buildOngoingAnime(42L);
            Anime refreshedAnime = buildOngoingAnime(42L);

            when(animeService.findById(42L)).thenReturn(Optional.of(ongoingAnime));
            when(animeService.refreshById(42L)).thenReturn(Optional.of(refreshedAnime));
            when(watchlistService.getWatchlist(USER_ID, 42L)).thenReturn(Optional.empty());
            when(messageBuilder.buildCaption(refreshedAnime, 0, null)).thenReturn("refreshed caption");
            when(messageBuilder.createInlineKeyboard(refreshedAnime, 0)).thenReturn(mock(InlineKeyboardMarkup.class));
            Mockito.when(telegramClient.execute(any(EditMessageText.class)))
                    .thenThrow(new org.telegram.telegrambots.meta.exceptions.TelegramApiException("edit failed"))
                    .thenReturn(null);

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
            verify(telegramClient, times(2)).execute(captor.capture());
            List<EditMessageText> edits = captor.getAllValues();
            assertThat(edits.get(1).getText()).isEqualTo("refreshed caption");
        }
    }
}
