package pro.zerobyte.animemizerbot.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shikimori")
public record ShikimoriProperties(
        String baseUrl
) {
    public String resolvedBaseUrl() {
        return (baseUrl == null || baseUrl.isBlank()) ? "https://shikimori.io" : baseUrl;
    }
}