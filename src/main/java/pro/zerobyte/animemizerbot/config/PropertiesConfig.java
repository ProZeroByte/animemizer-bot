package pro.zerobyte.animemizerbot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import pro.zerobyte.animemizerbot.config.props.AnilibertyProperties;
import pro.zerobyte.animemizerbot.config.props.KinopoiskProperties;
import pro.zerobyte.animemizerbot.config.props.OpenRouterProperties;
import pro.zerobyte.animemizerbot.config.props.ShikimoriProperties;
import pro.zerobyte.animemizerbot.config.props.TelegramBotProperties;

@Configuration
@EnableConfigurationProperties({
        TelegramBotProperties.class,
        ShikimoriProperties.class,
        KinopoiskProperties.class,
        AnilibertyProperties.class,
        OpenRouterProperties.class
})
public class PropertiesConfig {
}
