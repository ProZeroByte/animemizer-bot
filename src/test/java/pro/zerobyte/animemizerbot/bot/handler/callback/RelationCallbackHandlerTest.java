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
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.bot.message.AnimeMessageBuilder;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;
import pro.zerobyte.animemizerbot.core.watchlist.Watchlist;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationCallbackHandlerTest {

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

    private Anime buildAnime(Long id, String publicId, String prequelPublicId, String sequelPublicId) {
        return new Anime(id, publicId, prequelPublicId, sequelPublicId, "Test Anime", "Тест", null,
                null, null, null, false, false, false, 12, null, null, null, null, null, null, null);
    }

    private Anime buildRelatedAnime(Long id, String publicId, String name) {
        return new Anime(id, publicId, null, null, name, name, null,
                null, "http://poster.url", null, false, false, false, 12, null, null, null, null, null, null, null);
    }

    private Watchlist buildWatchlist(Long animeId, WatchStatus status, int lastWatchedEpisode) {
        return new Watchlist(1L, USER_ID, animeId, status, lastWatchedEpisode,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("canHandle")
    class CanHandle {

        @Test
        void should_returnTrue_when_loadPrequelPrefix() {
            // Given
            RelationCallbackHandler handler = new RelationCallbackHandler(
                    animeService, watchlistService, messageBuilder, telegramClient);

            // When
            boolean result = handler.canHandle("LOAD_PREQUEL:123");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnTrue_when_loadSequelPrefix() {
            // Given
            RelationCallbackHandler handler = new RelationCallbackHandler(
                    animeService, watchlistService, messageBuilder, telegramClient);

            // When
            boolean result = handler.canHandle("LOAD_SEQUEL:123");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnFalse_when_differentPrefix() {
            // Given
            RelationCallbackHandler handler = new RelationCallbackHandler(
                    animeService, watchlistService, messageBuilder, telegramClient);

            // When
            boolean result = handler.canHandle("NEXT_EPISODE:123");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("handle — prequel")
    class HandlePrequel {

        @Test
        void should_replaceWithPrequel_when_found() throws Exception {
            // Given
            RelationCallbackHandler handler = new RelationCallbackHandler(
                    animeService, watchlistService, messageBuilder, telegramClient);
            Update update = buildCallbackUpdate("LOAD_PREQUEL:42", USER_ID, false);
            Anime anime = buildAnime(42L, "shikimori_42", "shikimori_10", null);
            Anime prequelAnime = buildRelatedAnime(10L, "shikimori_10", "Предыстория");
            Watchlist watchlist = buildWatchlist(10L, WatchStatus.WATCHING, 0);

            when(animeService.findById(42L)).thenReturn(Optional.of(anime));
            when(animeService.findByPublicIdOrFetch("shikimori_10")).thenReturn(Optional.of(prequelAnime));
            when(watchlistService.getWatchlist(USER_ID, 10L)).thenReturn(Optional.of(watchlist));
            when(messageBuilder.buildCaption(prequelAnime, 0, WatchStatus.WATCHING)).thenReturn("prequel caption");
            when(messageBuilder.createInlineKeyboard(prequelAnime, 0)).thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(telegramClient, atLeastOnce()).execute(any(EditMessageText.class));
        }

        @Test
        void should_showNotFound_when_prequelDoesNotExist() throws Exception {
            // Given
            RelationCallbackHandler handler = new RelationCallbackHandler(
                    animeService, watchlistService, messageBuilder, telegramClient);
            Update update = buildCallbackUpdate("LOAD_PREQUEL:42", USER_ID, false);
            Anime anime = buildAnime(42L, "shikimori_42", "shikimori_10", null);

            when(animeService.findById(42L)).thenReturn(Optional.of(anime));
            when(animeService.findByPublicIdOrFetch("shikimori_10")).thenReturn(Optional.empty());

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(telegramClient, atLeastOnce()).execute(any(EditMessageText.class));
        }

        @Test
        void should_returnNull_when_invalidId() {
            // Given
            RelationCallbackHandler handler = new RelationCallbackHandler(
                    animeService, watchlistService, messageBuilder, telegramClient);
            Update update = buildCallbackUpdate("LOAD_PREQUEL:", USER_ID, false);

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("handle — sequel")
    class HandleSequel {

        @Test
        void should_replaceWithSequel_when_found() throws Exception {
            // Given
            RelationCallbackHandler handler = new RelationCallbackHandler(
                    animeService, watchlistService, messageBuilder, telegramClient);
            Update update = buildCallbackUpdate("LOAD_SEQUEL:42", USER_ID, false);
            Anime anime = buildAnime(42L, "shikimori_42", null, "shikimori_99");
            Anime sequelAnime = buildRelatedAnime(99L, "shikimori_99", "Продолжение");
            Watchlist watchlist = buildWatchlist(99L, WatchStatus.WATCHING, 0);

            when(animeService.findById(42L)).thenReturn(Optional.of(anime));
            when(animeService.findByPublicIdOrFetch("shikimori_99")).thenReturn(Optional.of(sequelAnime));
            when(watchlistService.getWatchlist(USER_ID, 99L)).thenReturn(Optional.of(watchlist));
            when(messageBuilder.buildCaption(sequelAnime, 0, WatchStatus.WATCHING)).thenReturn("sequel caption");
            when(messageBuilder.createInlineKeyboard(sequelAnime, 0)).thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(telegramClient, atLeastOnce()).execute(any(EditMessageText.class));
        }

        @Test
        void should_showNotFound_when_sequelDoesNotExist() throws Exception {
            // Given
            RelationCallbackHandler handler = new RelationCallbackHandler(
                    animeService, watchlistService, messageBuilder, telegramClient);
            Update update = buildCallbackUpdate("LOAD_SEQUEL:42", USER_ID, false);
            Anime anime = buildAnime(42L, "shikimori_42", null, "shikimori_99");

            when(animeService.findById(42L)).thenReturn(Optional.of(anime));
            when(animeService.findByPublicIdOrFetch("shikimori_99")).thenReturn(Optional.empty());

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(telegramClient, atLeastOnce()).execute(any(EditMessageText.class));
        }

        @Test
        void should_returnNull_when_invalidId() {
            // Given
            RelationCallbackHandler handler = new RelationCallbackHandler(
                    animeService, watchlistService, messageBuilder, telegramClient);
            Update update = buildCallbackUpdate("LOAD_SEQUEL:", USER_ID, false);

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
        }
    }
}

