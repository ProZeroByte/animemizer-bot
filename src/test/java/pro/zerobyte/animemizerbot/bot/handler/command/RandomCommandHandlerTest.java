package pro.zerobyte.animemizerbot.bot.handler.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RandomCommandHandlerTest {

    private static final Long USER_ID = 100L;
    private static final Long CHAT_ID = 12345L;

    @Mock
    private TelegramUserService telegramUserService;

    @Mock
    private AnimeCardSender animeCardSender;

    private Update buildUpdate() {
        Update update = Mockito.mock(Update.class);
        Message message = Mockito.mock(Message.class);
        User user = Mockito.mock(User.class);
        Mockito.lenient().when(update.getMessage()).thenReturn(message);
        Mockito.lenient().when(message.getChatId()).thenReturn(CHAT_ID);
        Mockito.lenient().when(message.getFrom()).thenReturn(user);
        Mockito.lenient().when(user.getId()).thenReturn(USER_ID);
        return update;
    }

    @Nested
    @DisplayName("getCommand")
    class GetCommand {

        @Test
        void should_returnRandom() {
            // Given
            RandomCommandHandler handler = new RandomCommandHandler(telegramUserService, animeCardSender);

            // When
            AnimemizerBotCommand result = handler.getCommand();

            // Then
            assertThat(result.getCommandName()).isEqualTo("/random");
        }
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        void should_registerUser_andSendRandomCard_when_userIsUnknown() {
            // Given
            RandomCommandHandler handler = new RandomCommandHandler(telegramUserService, animeCardSender);
            Update update = buildUpdate();
            when(telegramUserService.findByTelegramId(USER_ID)).thenReturn(Optional.empty());
            when(telegramUserService.createOrUpdate(update.getMessage().getFrom(), CHAT_ID))
                    .thenReturn(Mockito.mock(TelegramUser.class));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(telegramUserService).createOrUpdate(update.getMessage().getFrom(), CHAT_ID);
            verify(animeCardSender).sendRandomCard(CHAT_ID, USER_ID);
        }

        @Test
        void should_sendRandomCard_when_userAlreadyExists() {
            // Given
            RandomCommandHandler handler = new RandomCommandHandler(telegramUserService, animeCardSender);
            Update update = buildUpdate();
            when(telegramUserService.findByTelegramId(USER_ID))
                    .thenReturn(Optional.of(Mockito.mock(TelegramUser.class)));

            // When
            PartialBotApiMethod<?> result = handler.handle(update);

            // Then
            assertThat(result).isNull();
            verify(telegramUserService, Mockito.never()).createOrUpdate(Mockito.any(), Mockito.any());
            verify(animeCardSender).sendRandomCard(CHAT_ID, USER_ID);
        }
    }
}
