package pro.zerobyte.animemizerbot.bot.handler.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCommand;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeListService;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchingCommandHandlerTest {

    private static final Long CHAT_ID = 12345L;
    private static final Long USER_ID = 100L;

    @Mock
    private AnimeListService animeListService;

    @Mock
    private TelegramClient telegramClient;

    private Update buildUpdate() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        User user = mock(User.class);
        Mockito.lenient().when(update.getMessage()).thenReturn(message);
        Mockito.lenient().when(message.getChatId()).thenReturn(CHAT_ID);
        Mockito.lenient().when(message.getFrom()).thenReturn(user);
        Mockito.lenient().when(user.getId()).thenReturn(USER_ID);
        return update;
    }

    private List<AnimeListService.WatchingEntry> singleEntry() {
        Anime anime = new Anime(10L, "shikimori_10", null, null, "First", "Первое", null,
                null, null, null, false, false, true, 24, null, null, null, null, null, null);
        return List.of(new AnimeListService.WatchingEntry(anime, 5));
    }

    @Nested
    @DisplayName("getCommand")
    class GetCommand {

        @Test
        void should_returnWatchingCommand() {
            // Given
            WatchingCommandHandler handler = new WatchingCommandHandler(animeListService, telegramClient);

            // When
            AnimemizerBotCommand result = handler.getCommand();

            // Then
            assertThat(result.getCommandName()).isEqualTo("/watching");
        }
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        void should_editPlaceholderWithList_when_entriesExist() throws Exception {
            // Given
            WatchingCommandHandler handler = new WatchingCommandHandler(animeListService, telegramClient);
            Update update = buildUpdate();
            List<AnimeListService.WatchingEntry> entries = singleEntry();

            Message placeholder = mock(Message.class);
            when(placeholder.getMessageId()).thenReturn(123);
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);
            when(animeListService.fetchWatchingEntries(USER_ID)).thenReturn(entries);
            when(animeListService.buildWatchingPageText(entries, 0)).thenReturn("список смотрю");
            when(animeListService.totalPages(1)).thenReturn(1);
            when(animeListService.buildWatchingPageKeyboard(USER_ID, 0, 1))
                    .thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(telegramClient).execute(any(SendMessage.class));
            verify(telegramClient).execute(any(EditMessageText.class));
        }

        @Test
        void should_editPlaceholderWithEmptyMessage_when_noWatchingEntries() throws Exception {
            // Given
            WatchingCommandHandler handler = new WatchingCommandHandler(animeListService, telegramClient);
            Update update = buildUpdate();
            List<AnimeListService.WatchingEntry> entries = List.of();

            Message placeholder = mock(Message.class);
            when(placeholder.getMessageId()).thenReturn(123);
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);
            when(animeListService.fetchWatchingEntries(USER_ID)).thenReturn(entries);
            when(animeListService.buildWatchingPageText(entries, 0)).thenReturn("Вы пока ничего не смотрите");
            when(animeListService.totalPages(0)).thenReturn(0);
            when(animeListService.buildWatchingPageKeyboard(USER_ID, 0, 0)).thenReturn(null);

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(telegramClient).execute(any(EditMessageText.class));
        }

        @Test
        void should_returnFallbackMessage_when_placeholderFails() throws Exception {
            // Given
            WatchingCommandHandler handler = new WatchingCommandHandler(animeListService, telegramClient);
            Update update = buildUpdate();
            List<AnimeListService.WatchingEntry> entries = singleEntry();

            when(telegramClient.execute(any(SendMessage.class)))
                    .thenThrow(new TelegramApiException("network error"));
            when(animeListService.fetchWatchingEntries(USER_ID)).thenReturn(entries);
            when(animeListService.buildWatchingPageText(entries, 0)).thenReturn("список смотрю");
            when(animeListService.totalPages(1)).thenReturn(1);
            when(animeListService.buildWatchingPageKeyboard(USER_ID, 0, 1))
                    .thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isInstanceOf(SendMessage.class);
            assertThat(((SendMessage) result).getText()).isEqualTo("список смотрю");
        }
    }
}
