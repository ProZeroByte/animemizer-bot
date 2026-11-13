package pro.zerobyte.animemizerbot.bot.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.watchlist.WatchlistService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimeCardSenderTest {

    private static final Long CHAT_ID = 12345L;
    private static final Long USER_ID = 100L;

    @Mock
    private AnimeService animeService;

    @Mock
    private WatchlistService watchlistService;

    @Mock
    private AnimeMessageBuilder messageBuilder;

    @Mock
    private TelegramClient telegramClient;

    private AnimeCardSender createSender() {
        return new AnimeCardSender(animeService, watchlistService, messageBuilder,
                telegramClient, Runnable::run);
    }

    private Anime buildAnime(Long id, String posterUrl) {
        return new Anime(id, "shikimori_" + id, null, null, "Test Anime", "Тест", null,
                null, posterUrl, null, false, false, true, 12, 8, null, null, null, null, null);
    }

    private Message buildPlaceholder() {
        Message placeholder = mock(Message.class);
        when(placeholder.getMessageId()).thenReturn(777);
        return placeholder;
    }

    @Nested
    @DisplayName("sendCardByInternalId")
    class SendCardByInternalId {

        @Test
        void should_sendPhotoCard_andDeletePlaceholder_when_animeHasPoster() throws Exception {
            // Given
            AnimeCardSender sender = createSender();
            Anime anime = buildAnime(42L, "http://poster.url");

            Message placeholder = buildPlaceholder();
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);
            when(animeService.findById(42L)).thenReturn(Optional.of(anime));
            when(watchlistService.getWatchlist(USER_ID, 42L)).thenReturn(Optional.empty());
            when(messageBuilder.buildCaption(anime, 0, null)).thenReturn("caption");
            when(messageBuilder.createInlineKeyboard(anime, 0)).thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            sender.sendCardByInternalId(CHAT_ID, USER_ID, 42L);

            // Then
            ArgumentCaptor<SendPhoto> captor = ArgumentCaptor.forClass(SendPhoto.class);
            verify(telegramClient).execute(captor.capture());
            assertThat(captor.getValue().getCaption()).isEqualTo("caption");
            verify(telegramClient).execute(any(DeleteMessage.class));
        }

        @Test
        void should_editPlaceholderWithTextCard_when_noPoster() throws Exception {
            // Given
            AnimeCardSender sender = createSender();
            Anime anime = buildAnime(42L, null);

            Message placeholder = buildPlaceholder();
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);
            when(animeService.findById(42L)).thenReturn(Optional.of(anime));
            when(watchlistService.getWatchlist(USER_ID, 42L)).thenReturn(Optional.empty());
            when(messageBuilder.buildCaption(anime, 0, null)).thenReturn("caption");
            when(messageBuilder.createInlineKeyboard(anime, 0)).thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            sender.sendCardByInternalId(CHAT_ID, USER_ID, 42L);

            // Then
            ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
            verify(telegramClient).execute(captor.capture());
            assertThat(captor.getValue().getText()).isEqualTo("caption");
            verify(telegramClient, never()).execute(any(SendPhoto.class));
            verify(telegramClient, never()).execute(any(DeleteMessage.class));
        }

        @Test
        void should_fallBackToTextCard_when_sendPhotoFails() throws Exception {
            // Given
            AnimeCardSender sender = createSender();
            Anime anime = buildAnime(42L, "http://broken-poster.url");

            Message placeholder = buildPlaceholder();
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);
            when(animeService.findById(42L)).thenReturn(Optional.of(anime));
            when(watchlistService.getWatchlist(USER_ID, 42L)).thenReturn(Optional.empty());
            when(messageBuilder.buildCaption(anime, 0, null)).thenReturn("caption");
            when(messageBuilder.createInlineKeyboard(anime, 0)).thenReturn(mock(InlineKeyboardMarkup.class));
            Mockito.when(telegramClient.execute(any(SendPhoto.class)))
                    .thenThrow(new TelegramApiException("poster fetch failed"));

            // When
            sender.sendCardByInternalId(CHAT_ID, USER_ID, 42L);

            // Then
            verify(telegramClient).execute(any(EditMessageText.class));
            verify(telegramClient, never()).execute(any(DeleteMessage.class));
        }

        @Test
        void should_showNotFound_when_animeMissing() throws Exception {
            // Given
            AnimeCardSender sender = createSender();

            Message placeholder = buildPlaceholder();
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);
            when(animeService.findById(42L)).thenReturn(Optional.empty());

            // When
            sender.sendCardByInternalId(CHAT_ID, USER_ID, 42L);

            // Then
            ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
            verify(telegramClient).execute(captor.capture());
            assertThat(captor.getValue().getText()).isEqualTo("Аниме не найдено");
        }

        @Test
        void should_showError_when_serviceThrows() throws Exception {
            // Given
            AnimeCardSender sender = createSender();

            Message placeholder = buildPlaceholder();
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);
            when(animeService.findById(42L)).thenThrow(new RuntimeException("database down"));

            // When
            sender.sendCardByInternalId(CHAT_ID, USER_ID, 42L);

            // Then
            ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
            verify(telegramClient).execute(captor.capture());
            assertThat(captor.getValue().getText()).contains("ошибка");
        }

        @Test
        void should_sendCardDirectly_when_placeholderFails() throws Exception {
            // Given
            AnimeCardSender sender = createSender();
            Anime anime = buildAnime(42L, "http://poster.url");

            when(telegramClient.execute(any(SendMessage.class)))
                    .thenThrow(new TelegramApiException("network error"));
            when(animeService.findById(42L)).thenReturn(Optional.of(anime));
            when(watchlistService.getWatchlist(USER_ID, 42L)).thenReturn(Optional.empty());
            when(messageBuilder.buildCaption(anime, 0, null)).thenReturn("caption");
            when(messageBuilder.createInlineKeyboard(anime, 0)).thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            sender.sendCardByInternalId(CHAT_ID, USER_ID, 42L);

            // Then — no placeholder to edit: the card is sent as a new message
            verify(telegramClient).execute(any(SendPhoto.class));
            verify(telegramClient, never()).execute(any(DeleteMessage.class));
        }
    }

    @Nested
    @DisplayName("sendCardByPublicId")
    class SendCardByPublicId {

        @Test
        void should_fetchByPublicId_andSendCard() throws Exception {
            // Given
            AnimeCardSender sender = createSender();
            Anime anime = buildAnime(42L, "http://poster.url");

            Message placeholder = buildPlaceholder();
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(placeholder);
            when(animeService.findByPublicIdOrFetch("shikimori_42")).thenReturn(Optional.of(anime));
            when(watchlistService.getWatchlist(USER_ID, 42L)).thenReturn(Optional.empty());
            when(messageBuilder.buildCaption(anime, 0, null)).thenReturn("caption");
            when(messageBuilder.createInlineKeyboard(anime, 0)).thenReturn(mock(InlineKeyboardMarkup.class));

            // When
            sender.sendCardByPublicId(CHAT_ID, USER_ID, "shikimori_42");

            // Then
            verify(animeService).findByPublicIdOrFetch("shikimori_42");
            verify(telegramClient).execute(any(SendPhoto.class));
            verify(telegramClient).execute(any(DeleteMessage.class));
        }
    }
}
