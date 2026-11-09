package pro.zerobyte.animemizerbot.config.props;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "openrouter")
public record OpenRouterProperties(
        @NotBlank String apiKey,
        String baseUrl,
        String model
) {
    public String resolvedBaseUrl() {
        return (baseUrl == null || baseUrl.isBlank()) ? "https://openrouter.ai/api/v1" : baseUrl;
    }

    public String resolvedModel() {
        return (model == null || model.isBlank()) ? "openai/gpt-4o-mini" : model;
    }
}
