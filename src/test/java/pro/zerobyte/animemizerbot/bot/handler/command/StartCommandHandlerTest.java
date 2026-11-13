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
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCommand;
import pro.zerobyte.animemizerbot.bot.message.AnimeCardSender;
import pro.zerobyte.animemizerbot.core.telegramuser.TelegramUser;
import pro.zerobyte.animemizerbot.core.telegramuser.TelegramUserService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartCommandHandlerTest {

    private static final Long USER_ID = 100L;
    private static final Long CHAT_ID = 12345L;

    @Mock
    private TelegramUserService telegramUserService;

    @Mock
    private AnimeCardSender animeCardSender;

    private Update buildUpdate(String firstName, String text) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        User user = mock(User.class);
        Mockito.lenient().when(update.getMessage()).thenReturn(message);
        Mockito.lenient().when(message.getChatId()).thenReturn(CHAT_ID);
        Mockito.lenient().when(message.getFrom()).thenReturn(user);
        Mockito.lenient().when(message.getText()).thenReturn(text);
        Mockito.lenient().when(user.getId()).thenReturn(USER_ID);
        Mockito.lenient().when(user.getFirstName()).thenReturn(firstName);
        return update;
    }

    private TelegramUser buildTelegramUser(String firstName) {
        return new TelegramUser(1L, USER_ID, CHAT_ID, "testuser", firstName, "Last",
                LocalDateTime.now(), true);
    }

    @Nested
    @DisplayName("getCommand")
    class GetCommand {

        @Test
        void should_returnStart() {
            // Given
            StartCommandHandler handler = new StartCommandHandler(telegramUserService, animeCardSender);

            // When
            AnimemizerBotCommand result = handler.getCommand();

            // Then
            assertThat(result.getCommandName()).isEqualTo("/start");
        }
    }

    @Nested
    @DisplayName("handle — deep links")
    class HandleDeepLinks {

        @Test
        void should_openCard_when_showAnimePayload() {
            // Given
            StartCommandHandler handler = new StartCommandHandler(telegramUserService, animeCardSender);
            Update update = buildUpdate("TestUser", "/start show_42");
            when(telegramUserService.findByTelegramId(USER_ID))
                    .thenReturn(Optional.of(buildTelegramUser("ExistingName")));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(animeCardSender).sendCardByPublicId(CHAT_ID, USER_ID, "shikimori_42");
            verify(telegramUserService, never()).createOrUpdate(any(), any());
        }

        @Test
        void should_registerUserAndOpenCard_when_showAnimePayloadFromNewUser() {
            // Given
            StartCommandHandler handler = new StartCommandHandler(telegramUserService, animeCardSender);
            Update update = buildUpdate("NewUser", "/start show_42");
            when(telegramUserService.findByTelegramId(USER_ID)).thenReturn(Optional.empty());
            when(telegramUserService.createOrUpdate(any(), eq(CHAT_ID)))
                    .thenReturn(buildTelegramUser("NewUser"));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(telegramUserService).createOrUpdate(any(), eq(CHAT_ID));
            verify(animeCardSender).sendCardByPublicId(CHAT_ID, USER_ID, "shikimori_42");
        }

        @Test
        void should_openCard_when_showAnimePayloadWithBotMention() {
            // Given
            StartCommandHandler handler = new StartCommandHandler(telegramUserService, animeCardSender);
            Update update = buildUpdate("TestUser", "/start@animemizer_bot show_42");
            when(telegramUserService.findByTelegramId(USER_ID))
                    .thenReturn(Optional.of(buildTelegramUser("ExistingName")));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(animeCardSender).sendCardByPublicId(CHAT_ID, USER_ID, "shikimori_42");
        }

        @Test
        void should_returnWelcome_when_payloadIsNotShowAnime() {
            // Given
            StartCommandHandler handler = new StartCommandHandler(telegramUserService, animeCardSender);
            Update update = buildUpdate("TestUser", "/start hello");
            when(telegramUserService.findByTelegramId(USER_ID))
                    .thenReturn(Optional.of(buildTelegramUser("ExistingName")));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isInstanceOf(SendMessage.class);
            verifyNoInteractions(animeCardSender);
        }
    }

    @Nested
    @DisplayName("handle — plain start")
    class HandlePlainStart {

        @Test
        void should_returnWelcomeBack_when_existingUser() {
            // Given
            StartCommandHandler handler = new StartCommandHandler(telegramUserService, animeCardSender);
            Update update = buildUpdate("TestUser", "/start");
            TelegramUser existingUser = buildTelegramUser("ExistingName");
            when(telegramUserService.findByTelegramId(USER_ID)).thenReturn(Optional.of(existingUser));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isInstanceOf(SendMessage.class);
            assertThat(((SendMessage) result).getText()).contains("ExistingName");
            verify(telegramUserService, never()).createOrUpdate(any(), any());
        }

        @Test
        void should_createUserAndReturnWelcome_when_newUser() {
            // Given
            StartCommandHandler handler = new StartCommandHandler(telegramUserService, animeCardSender);
            Update update = buildUpdate("NewUser", "/start");
            when(telegramUserService.findByTelegramId(USER_ID)).thenReturn(Optional.empty());
            when(telegramUserService.createOrUpdate(any(), eq(CHAT_ID))).thenReturn(buildTelegramUser("NewUser"));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isInstanceOf(SendMessage.class);
            assertThat(((SendMessage) result).getText()).contains("NewUser");
            verify(telegramUserService).createOrUpdate(any(), eq(CHAT_ID));
        }
    }
}
