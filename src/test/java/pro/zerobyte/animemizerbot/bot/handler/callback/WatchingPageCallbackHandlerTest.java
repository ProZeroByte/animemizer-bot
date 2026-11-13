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
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeListService;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchingPageCallbackHandlerTest {

    private static final Long CHAT_ID = 12345L;
    private static final int MESSAGE_ID = 100;

    @Mock
    private AnimeListService animeListService;

    @Mock
    private TelegramClient telegramClient;

    private Update buildCallbackUpdate(String data, long clickerId) {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        User user = mock(User.class);
        Mockito.lenient().when(update.getCallbackQuery()).thenReturn(callbackQuery);
        Mockito.lenient().when(callbackQuery.getData()).thenReturn(data);
        Mockito.lenient().when(callbackQuery.getFrom()).thenReturn(user);
        Mockito.lenient().when(user.getId()).thenReturn(clickerId);
        Mockito.lenient().when(callbackQuery.getMessage()).thenReturn(message);
        Mockito.lenient().when(message.getChatId()).thenReturn(CHAT_ID);
        Mockito.lenient().when(message.getMessageId()).thenReturn(MESSAGE_ID);
        return update;
    }

    private List<AnimeListService.WatchingEntry> entries(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> new AnimeListService.WatchingEntry(
                        new Anime((long) i, "shikimori_" + i, null, null, "Anime " + i, "Аниме " + i, null,
                                null, null, null, false, false, true, 12, null, null, null, null, null, null), 1))
                .toList();
    }

    @Nested
    @DisplayName("canHandle")
    class CanHandle {

        @Test
        void should_returnTrue_when_watchingPagePrefix() {
            // Given
            WatchingPageCallbackHandler handler = new WatchingPageCallbackHandler(animeListService, telegramClient);

            // When
            boolean result = handler.canHandle("WATCHING_PAGE:100:0");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_returnFalse_when_differentPrefix() {
            // Given
            WatchingPageCallbackHandler handler = new WatchingPageCallbackHandler(animeListService, telegramClient);

            // When
            boolean result = handler.canHandle("LIST_PAGE:100:0");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        void should_replaceWithRequestedPage_when_validCallback() throws Exception {
            // Given
            WatchingPageCallbackHandler handler = new WatchingPageCallbackHandler(animeListService, telegramClient);
            Update update = buildCallbackUpdate("WATCHING_PAGE:100:1", 100L);
            List<AnimeListService.WatchingEntry> entries = entries(25);

            when(animeListService.fetchWatchingEntries(100L)).thenReturn(entries);
            when(animeListService.totalPages(25)).thenReturn(2);
            when(animeListService.buildWatchingPageText(entries, 1)).thenReturn("страница 2");
            when(animeListService.buildWatchingPageKeyboard(100L, 1, 2))
                    .thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(telegramClient, atLeastOnce()).execute(any(EditMessageText.class));
            verify(animeListService).buildWatchingPageText(entries, 1);
        }

        @Test
        void should_showEmptyMessage_when_noEntries() throws Exception {
            // Given
            WatchingPageCallbackHandler handler = new WatchingPageCallbackHandler(animeListService, telegramClient);
            Update update = buildCallbackUpdate("WATCHING_PAGE:100:0", 100L);

            when(animeListService.fetchWatchingEntries(100L)).thenReturn(List.of());
            when(animeListService.totalPages(0)).thenReturn(0);

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(telegramClient, atLeastOnce()).execute(any(EditMessageText.class));
        }

        @Test
        void should_clampPageIndex_when_outOfRange() throws Exception {
            // Given
            WatchingPageCallbackHandler handler = new WatchingPageCallbackHandler(animeListService, telegramClient);
            Update update = buildCallbackUpdate("WATCHING_PAGE:100:5", 100L);
            List<AnimeListService.WatchingEntry> entries = entries(3);

            when(animeListService.fetchWatchingEntries(100L)).thenReturn(entries);
            when(animeListService.totalPages(3)).thenReturn(1);
            when(animeListService.buildWatchingPageText(entries, 0)).thenReturn("страница 1");
            when(animeListService.buildWatchingPageKeyboard(100L, 0, 1))
                    .thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(animeListService).buildWatchingPageText(entries, 0);
        }

        @Test
        void should_doNothing_when_clickerIsNotOwner() {
            // Given
            WatchingPageCallbackHandler handler = new WatchingPageCallbackHandler(animeListService, telegramClient);
            Update update = buildCallbackUpdate("WATCHING_PAGE:100:0", 999L);

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verifyNoInteractions(animeListService);
            verifyNoInteractions(telegramClient);
        }

        @Test
        void should_doNothing_when_invalidPayload() {
            // Given
            WatchingPageCallbackHandler handler = new WatchingPageCallbackHandler(animeListService, telegramClient);
            Update update = buildCallbackUpdate("WATCHING_PAGE:abc", 100L);

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verifyNoInteractions(animeListService);
            verifyNoInteractions(telegramClient);
        }
    }
}
