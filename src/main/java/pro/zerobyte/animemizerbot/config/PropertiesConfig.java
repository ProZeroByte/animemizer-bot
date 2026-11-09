package pro.zerobyte.animemizerbot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import pro.zerobyte.animemizerbot.config.props.AnimeUrlProperties;
import pro.zerobyte.animemizerbot.config.props.OpenRouterProperties;
import pro.zerobyte.animemizerbot.config.props.TavilyProperties;
import pro.zerobyte.animemizerbot.config.props.TelegramBotProperties;

@Configuration
@EnableConfigurationProperties({
        TelegramBotProperties.class,
        AnimeUrlProperties.class,
        TavilyProperties.class,
        OpenRouterProperties.class
})
public class PropertiesConfig {
}
