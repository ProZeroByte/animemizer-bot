package pro.zerobyte.animemizerbot.bot.handler.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HelpCommandHandlerTest {

    private static final Long CHAT_ID = 12345L;

    private Update buildUpdate() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(CHAT_ID);
        return update;
    }

    @Nested
    @DisplayName("getCommand")
    class GetCommand {

        @Test
        void should_returnHelp() {
            // Given
            HelpCommandHandler handler = new HelpCommandHandler();

            // When
            AnimemizerBotCommand result = handler.getCommand();

            // Then
            assertThat(result.getCommandName()).isEqualTo("/help");
        }
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        void should_returnMessageContainingAllCommands() {
            // Given
            HelpCommandHandler handler = new HelpCommandHandler();
            Update update = buildUpdate();

            // When
            SendMessage result = handler.handle(update);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getText()).contains("/start");
            assertThat(result.getText()).contains("/help");
            assertThat(result.getText()).contains("/stop");
        }
    }
}
