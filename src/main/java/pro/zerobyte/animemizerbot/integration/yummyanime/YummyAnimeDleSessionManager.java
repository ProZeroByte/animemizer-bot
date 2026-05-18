package pro.zerobyte.animemizerbot.integration.yummyanime;

import pro.zerobyte.animemizerbot.config.CacheConfig;
import pro.zerobyte.animemizerbot.config.props.NetworkProperties;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class YummyAnimeDleSessionManager {

    private static final Logger log = LoggerFactory.getLogger(YummyAnimeDleSessionManager.class);

    private final NetworkProperties networkProperties;
    private final Pattern dleLoginHashPattern;

    public record DleSession(String loginHash, Map<String, String> cookies, String userAgent, int timeoutMs) {}

    public YummyAnimeDleSessionManager(NetworkProperties networkProperties) {
        this.networkProperties = networkProperties;
        this.dleLoginHashPattern = Pattern.compile("dle_login_hash\\s*=\\s*['\"]([a-f0-9]{40})['\"]");
    }

    @Cacheable(value = CacheConfig.YUMMY_SESSIONS, key = "#baseUrl", unless = "#result == null")
    public Optional<DleSession> getOrCreateSession(String baseUrl) {
        try {
            String currentAgent = networkProperties.getRandomAgent();

            Connection.Response response = Jsoup.connect(baseUrl)
                    .userAgent(currentAgent)
                    .timeout(networkProperties.providerTimeoutMs())
                    .method(Connection.Method.GET)
                    .execute();

            Matcher matcher = dleLoginHashPattern.matcher(response.body());
            if (matcher.find()) {
                log.info("Successfully fetched new DLE session for {}", baseUrl);
                return Optional.of(new DleSession(matcher.group(1), response.cookies(), currentAgent, networkProperties.providerTimeoutMs()));
            } else {
                log.warn("Failed to find dle_login_hash on {}.", baseUrl);
            }
        } catch (IOException e) {
            log.error("Network error while fetching DLE session for {}: {}", baseUrl, e.getMessage());
        }
        return Optional.empty();
    }
}
