package pro.zerobyte.animemizerbot.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCommand;
import pro.zerobyte.animemizerbot.bot.processor.BotUpdateProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class TelegramBotService {
    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    private final BotUpdateProcessor botUpdateProcessor;
    private final TelegramClient telegramClient;
    private final AsyncTaskExecutor taskExecutor;
    private final Optional<SetWebhook> setWebhook;

    public TelegramBotService(BotUpdateProcessor botUpdateProcessor,
                              TelegramClient telegramClient,
                              @Qualifier("applicationTaskExecutor") AsyncTaskExecutor taskExecutor,
                              Optional<SetWebhook> setWebhook) {
        this.botUpdateProcessor = botUpdateProcessor;
        this.telegramClient = telegramClient;
        this.taskExecutor = taskExecutor;
        this.setWebhook = setWebhook;
    }

    /**
     * Central update handler for both Polling and Webhook modes.
     * Delegates processing to an async executor.
     */
    public void consume(Update update) {
        taskExecutor.submit(() -> {
            try {
                botUpdateProcessor.processUpdate(update);
            } catch (Exception e) {
                // Broad catch at async task boundary is intentional — prevents silent thread death
                // on virtual threads. processUpdate() can throw various unchecked exceptions.
                log.error("Error processing update", e);
            }
        });
    }

    /**
     * Registers bot menu commands.
     * Called after the application is fully started.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        registerBotMenu();
        setWebhook.ifPresent(this::registerWebhook);
    }

    private void registerBotMenu() {
        try {
            List<BotCommand> telegramCommands = Arrays.stream(AnimemizerBotCommand.values())
                    .map(cmd -> new BotCommand(cmd.getCommandName(), cmd.getDescription()))
                    .toList();

            SetMyCommands setMyCommands = new SetMyCommands(telegramCommands, new BotCommandScopeDefault(), null);
            telegramClient.execute(setMyCommands);
            log.info("Bot menu commands registered successfully");
        } catch (TelegramApiException e) {
            log.error("Failed to register bot menu commands: {}", e.getMessage());
        }
    }

    private void registerWebhook(SetWebhook webhook) {
        try {
            log.info("Attempting to register Webhook at: {}", webhook.getUrl());
            telegramClient.execute(webhook);
            log.info("Telegram Webhook registered successfully!");
        } catch (TelegramApiException e) {
            log.error("Failed to register Telegram Webhook", e);
        }
    }
}
