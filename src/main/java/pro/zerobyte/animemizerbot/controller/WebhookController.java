package pro.zerobyte.animemizerbot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;
import pro.zerobyte.animemizerbot.bot.TelegramBotService;
import pro.zerobyte.animemizerbot.config.props.TelegramBotProperties;

@RestController
@Profile("prod")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final TelegramBotService telegramBotService;
    private final TelegramBotProperties properties;

    public WebhookController(TelegramBotService telegramBotService,
                             TelegramBotProperties properties) {
        this.telegramBotService = telegramBotService;
        this.properties = properties;
    }

    @PostMapping(value = "${telegram.bot.webhook-path}")
    public void onWebhookUpdate(
            @RequestBody Update update,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretKey) {

        if (properties.secretToken() != null && !properties.secretToken().equals(secretKey)) {
            log.warn("Received unauthorized webhook request [Update ID: {}]. Secret key mismatch.", update.getUpdateId());
            return;
        }

        log.debug("Received Webhook Update [ID: {}], delegating to TelegramBotService", update.getUpdateId());
        telegramBotService.consume(update);
    }
}
