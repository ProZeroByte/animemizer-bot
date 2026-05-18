package pro.zerobyte.animemizerbot.config.props;

import pro.zerobyte.animemizerbot.core.anime.AnimeSource;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app")
public record AnimeUrlProperties(
        Map<AnimeSource, String> animeSources,
        String watchUrlTemplate
) {

    public String getUrlFor(AnimeSource source) {
        return animeSources.getOrDefault(source, "");
    }
}
