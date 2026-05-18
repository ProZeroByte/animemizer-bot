package pro.zerobyte.animemizerbot.config.props;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "telegram.bot")
public record TelegramBotProperties(
        @NotBlank String token,
        @NotBlank String username,
        String webhookPath,
        String webhookBaseUrl,
        String secretToken
) {}
