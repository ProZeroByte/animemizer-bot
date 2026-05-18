package pro.zerobyte.animemizerbot.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ConfigurationProperties(prefix = "app.network")
public record NetworkProperties(int providerTimeoutMs, List<String> userAgents
) {
    public String getRandomAgent() {
        return userAgents.get(ThreadLocalRandom.current().nextInt(userAgents.size()));
    }
}
