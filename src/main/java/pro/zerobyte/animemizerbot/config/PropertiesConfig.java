package pro.zerobyte.animemizerbot.config;

import pro.zerobyte.animemizerbot.config.props.AnimeUrlProperties;
import pro.zerobyte.animemizerbot.config.props.NetworkProperties;
import pro.zerobyte.animemizerbot.config.props.TelegramBotProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        TelegramBotProperties.class,
        AnimeUrlProperties.class,
        NetworkProperties.class
})
public class PropertiesConfig {
}
