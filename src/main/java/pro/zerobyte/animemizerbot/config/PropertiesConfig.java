package pro.zerobyte.animemizerbot.config;

import pro.zerobyte.animemizerbot.config.props.AnimeUrlProperties;
import pro.zerobyte.animemizerbot.config.props.TavilyProperties;
import pro.zerobyte.animemizerbot.config.props.TelegramBotProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        TelegramBotProperties.class,
        AnimeUrlProperties.class,
        TavilyProperties.class
})
public class PropertiesConfig {
}
