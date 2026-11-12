package pro.zerobyte.animemizerbot.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aniliberty")
public record AnilibertyProperties(
        String baseUrl
) {
    public String resolvedBaseUrl() {
        return (baseUrl == null || baseUrl.isBlank()) ? "https://www.aniliberty.top" : baseUrl;
    }
}

