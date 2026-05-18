package pro.zerobyte.animemizerbot.integration.animego;

import pro.zerobyte.animemizerbot.config.props.AnimeUrlProperties;
import pro.zerobyte.animemizerbot.config.props.NetworkProperties;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeSource;
import pro.zerobyte.animemizerbot.core.anime.AnimeWatchProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class AnimegoProvider implements AnimeWatchProvider {
    private static final Logger log = LoggerFactory.getLogger(AnimegoProvider.class);

    private final AnimeUrlProperties animeUrlProperties;
    private final NetworkProperties networkProperties;

    public AnimegoProvider(AnimeUrlProperties animeUrlProperties, NetworkProperties networkProperties) {
        this.animeUrlProperties = animeUrlProperties;
        this.networkProperties = networkProperties;
    }

    @Override
    public AnimeSource getSource() {
        return AnimeSource.ANIMEGO;
    }

    @Override
    public Optional<String> findExternalId(Anime anime) {
        String title = anime.name();
        String russianTitle = anime.russianName();

        if (title == null || title.isBlank() || russianTitle == null || russianTitle.isBlank()) {
            return Optional.empty();
        }

        try {
            String baseUrl = animeUrlProperties.getUrlFor(getSource());
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String searchUrl = "%s/search/all?q=%s".formatted(baseUrl, encodedTitle);

            Elements animeItems = Jsoup.connect(searchUrl)
                    .userAgent(networkProperties.getRandomAgent())
                    .timeout(networkProperties.providerTimeoutMs())
                    .get()
                    .select("div.ani-grid__item:has(div.ani-grid__item-title a[href^='/anime/'])");

            if (animeItems.isEmpty()) {
                log.info("Failed to extract externalId for anime '{}': No items returned from request", title);
                return Optional.empty();
            }

            String normalizedRussianTitle = russianTitle.replace("ё", "е").replace("Ё", "Е");

            Optional<String> externalId = animeItems.stream()
                    .map(item -> extractHrefIfMatches(item, russianTitle, normalizedRussianTitle))
                    .flatMap(Optional::stream)
                    .findFirst()
                    .map(href -> href.substring(href.lastIndexOf('/') + 1))
                    .map(id -> "anime/" + id);

            externalId.ifPresentOrElse(
                    id -> log.info("Successfully extracted externalId '{}' for anime '{}'", id, title),
                    () -> log.info("Failed to extract externalId for anime '{}': No exact match found in {} items", title, animeItems.size())
            );

            return externalId;
        } catch (IOException e) {
            log.warn("AnimeGo API error for query '{}': {}", title, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> extractHrefIfMatches(Element item, String exactTitle, String normalizedTitle) {
        Element linkTag = item.selectFirst("div.ani-grid__item-title a");
        if (linkTag == null) {
            return Optional.empty();
        }

        // Читаем текст из DOM один раз
        String mainTitle = linkTag.text().trim();
        if (mainTitle.equalsIgnoreCase(exactTitle) || mainTitle.equalsIgnoreCase(normalizedTitle)) {
            return Optional.of(linkTag.attr("href"));
        }

        // Ищем альтернативное название только если основное не совпало
        Element subTitleTag = item.selectFirst("div.fw-lighter");
        if (subTitleTag != null) {
            String subTitle = subTitleTag.text().trim();
            if (subTitle.equalsIgnoreCase(exactTitle) || subTitle.equalsIgnoreCase(normalizedTitle)) {
                return Optional.of(linkTag.attr("href"));
            }
        }

        return Optional.empty();
    }
}
