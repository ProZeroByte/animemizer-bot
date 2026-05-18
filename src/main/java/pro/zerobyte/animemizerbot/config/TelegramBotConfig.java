package pro.zerobyte.animemizerbot.config;

import pro.zerobyte.animemizerbot.bot.TelegramBotService;
import pro.zerobyte.animemizerbot.config.props.TelegramBotProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.telegram.telegrambots.client.jetty.JettyTelegramClient;
import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class TelegramBotConfig {

    /**
     * Общий клиент для отправки ответов в Telegram.
     * Одинаковый для dev (long-polling) и prod (webhook).
     * Skipped when a test provides its own TelegramClient mock via @Import.
     */
    @Bean
    @ConditionalOnMissingBean(TelegramClient.class)
    public TelegramClient telegramClient(TelegramBotProperties properties) {
        return new JettyTelegramClient(properties.token());
    }

    /**
     * Long-polling bot wrapper — только для dev.
     * Обеспечивает мостик для TelegramBotService как SpringLongPollingBot, чтобы
     * telegrambots-springboot-longpolling-starter активировал опрос.
     * В prod этот бин отсутствует — только вебхук получает обновления.
     */
    @Bean
    @Profile("dev")
    public org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot longPollingBot(
            TelegramBotProperties properties,
            TelegramBotService telegramBotService) {
        return new org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot() {
            @Override
            public String getBotToken() {
                return properties.token();
            }

            @Override
            public DefaultLongPollingUpdateConsumer getUpdatesConsumer() {
                return new DefaultLongPollingUpdateConsumer() {
                    @Override
                    public void consume(Update update) {
                        telegramBotService.consume(update);
                    }
                };
            }
        };
    }

    /**
     * Конфигурация Webhook — только для prod.
     * Определяет URL, по которому Telegram будет присылать обновления.
     */
    @Bean
    @Profile("prod")
    public SetWebhook setWebhook(TelegramBotProperties properties) {
        String baseUrl = trimTrailingSlashes(properties.webhookBaseUrl());
        String path = normalizeLeadingSlash(properties.webhookPath());
        String fullUrl = baseUrl + path;

        return SetWebhook.builder()
                .url(fullUrl)
                .secretToken(properties.secretToken())
                .dropPendingUpdates(true) // Очищаем застрявшие апдейты при рестарте
                .maxConnections(40)       // Оптимизация пула коннектов Telegram
                .build();
    }

    private String trimTrailingSlashes(String value) {
        if (value == null) return "";
        return value.stripTrailing().replaceAll("/+$", "");
    }

    private String normalizeLeadingSlash(String value) {
        if (value == null || value.isEmpty()) return "";
        String stripped = value.stripLeading().replaceAll("^/+", "");
        return stripped.isEmpty() ? "" : "/" + stripped;
    }
}
