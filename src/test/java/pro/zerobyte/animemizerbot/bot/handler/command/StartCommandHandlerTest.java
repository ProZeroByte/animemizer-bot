package pro.zerobyte.animemizerbot.bot.handler.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCommand;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartCommandHandlerTest {

    private static final Long USER_ID = 100L;
    private static final Long CHAT_ID = 12345L;

    @Mock
    private TelegramUserService telegramUserService;

    private Update buildUpdate(String firstName) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        User user = mock(User.class);
        Mockito.lenient().when(update.getMessage()).thenReturn(message);
        Mockito.lenient().when(message.getChatId()).thenReturn(CHAT_ID);
        Mockito.lenient().when(message.getFrom()).thenReturn(user);
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
            StartCommandHandler handler = new StartCommandHandler(telegramUserService);

            // When
            AnimemizerBotCommand result = handler.getCommand();

            // Then
            assertThat(result.getCommandName()).isEqualTo("/start");
        }
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        void should_returnWelcomeBack_when_existingUser() {
            // Given
            StartCommandHandler handler = new StartCommandHandler(telegramUserService);
            Update update = buildUpdate("TestUser");
            TelegramUser existingUser = buildTelegramUser("ExistingName");
            when(telegramUserService.findByTelegramId(USER_ID)).thenReturn(Optional.of(existingUser));

            // When
            SendMessage result = handler.handle(update);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getText()).contains("ExistingName");
            verify(telegramUserService, never()).createOrUpdate(any(), any());
        }

        @Test
        void should_createUserAndReturnWelcome_when_newUser() {
            // Given
            StartCommandHandler handler = new StartCommandHandler(telegramUserService);
            Update update = buildUpdate("NewUser");
            when(telegramUserService.findByTelegramId(USER_ID)).thenReturn(Optional.empty());
            when(telegramUserService.createOrUpdate(any(), eq(CHAT_ID))).thenReturn(buildTelegramUser("NewUser"));

            // When
            SendMessage result = handler.handle(update);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getText()).contains("NewUser");
            verify(telegramUserService).createOrUpdate(any(), eq(CHAT_ID));
        }
    }
}
