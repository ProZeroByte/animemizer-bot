package pro.zerobyte.animemizerbot.integration.yummyanime;

import pro.zerobyte.animemizerbot.config.props.AnimeUrlProperties;
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
import java.util.Optional;

@Service
public class YummyAnimeProvider implements AnimeWatchProvider {

    private static final Logger log = LoggerFactory.getLogger(YummyAnimeProvider.class);

    private final AnimeUrlProperties animeUrlProperties;
    private final YummyAnimeDleSessionManager yummyAnimeDleSessionManager;

    public YummyAnimeProvider(AnimeUrlProperties animeUrlProperties, YummyAnimeDleSessionManager yummyAnimeDleSessionManager) {
        this.animeUrlProperties = animeUrlProperties;
        this.yummyAnimeDleSessionManager = yummyAnimeDleSessionManager;
    }

    @Override
    public AnimeSource getSource() {
        return AnimeSource.YUMMYANIME;
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
            String searchUrl = baseUrl + "/engine/ajax/controller.php?mod=search";
            Optional<YummyAnimeDleSessionManager.DleSession> session = yummyAnimeDleSessionManager.getOrCreateSession(baseUrl);

            if (session.isEmpty()) {
                return Optional.empty();
            }

            YummyAnimeDleSessionManager.DleSession activeSession = session.get();

            Elements animeItems = Jsoup.connect(searchUrl)
                    .userAgent(activeSession.userAgent())
                    .timeout(activeSession.timeoutMs())
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("Referer", baseUrl)
                    .header("Origin", baseUrl)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .cookies(activeSession.cookies())
                    .data("query", title)
                    .data("skin", "New")
                    .data("user_hash", activeSession.loginHash())
                    .post()
                    .select("a.fast-search-result-item");

            if (animeItems.isEmpty()) {
                log.info("Failed to extract externalId for anime '{}': No items returned from request", title);
                return Optional.empty();
            }

            Optional<String> externalId = animeItems.stream()
                    .filter(item -> {
                        Element titleTag = item.selectFirst("b");
                        return titleTag != null && titleTag.text().trim().equalsIgnoreCase(russianTitle);
                    })
                    .findFirst()
                    .map(element -> element.attr("href"))
                    .map(href -> href.substring(href.lastIndexOf('/') + 1));

            externalId.ifPresentOrElse(
                    id -> log.info("Successfully extracted externalId '{}' for anime '{}'", id, title),
                    () -> log.info("Failed to extract externalId for anime '{}': No exact match found in {} items", title, animeItems.size())
            );

            return externalId;

        } catch (IOException e) {
            log.error("Error searching YummyAnime: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
