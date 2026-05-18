package pro.zerobyte.animemizerbot.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@TestConfiguration
public class TestTelegramConfig {

    @Bean
    @Primary
    public TelegramClient telegramClient() {
        return Mockito.mock(TelegramClient.class);
    }
}
