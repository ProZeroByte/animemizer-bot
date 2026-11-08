package pro.zerobyte.animemizerbot.config.props;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "tavily")
public record TavilyProperties(
        @NotBlank String apiKey,
        String baseUrl
) {
    public String resolvedBaseUrl() {
        return (baseUrl == null || baseUrl.isBlank()) ? "https://api.tavily.com" : baseUrl;
    }
}
