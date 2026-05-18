package pro.zerobyte.animemizerbot.integration.anilibria;

import org.springframework.web.client.HttpClientErrorException;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeSource;
import pro.zerobyte.animemizerbot.core.anime.AnimeWatchProvider;
import pro.zerobyte.animemizerbot.integration.anilibria.dto.AnilibriaReleaseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AnilibriaProvider implements AnimeWatchProvider {

    private static final Logger log = LoggerFactory.getLogger(AnilibriaProvider.class);

    private final AnilibriaClient anilibriaClient;

    public AnilibriaProvider(AnilibriaClient anilibriaClient) {
        this.anilibriaClient = anilibriaClient;
    }

    @Override
    public AnimeSource getSource() {
        return AnimeSource.ANILIBRIA;
    }

    @Override
    public Optional<String> findExternalId(Anime anime) {
        String title = anime.name();
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        try {
            List<AnilibriaReleaseDto> releases = anilibriaClient.searchReleases(title, "id,name.main,name.english");
            if (releases == null || releases.isEmpty()) {
                log.debug("Failed to extract externalId for anime '{}': No releases returned from API", title);
                return Optional.empty();
            }

            Optional<String> externalId = releases.stream()
                    .filter(release -> isExactMatch(release, title))
                    .findFirst()
                    .map(release -> "anime/releases/release/" + release.id());

            externalId.ifPresentOrElse(
                    id -> log.debug("Successfully extracted externalId '{}' for anime '{}'", id, title),
                    () -> log.debug("Failed to extract externalId for anime '{}': No exact match found in {} releases", title, releases.size()));

            return externalId;
        } catch (HttpClientErrorException e) {
            log.warn("Anilibria API error for query '{}': {} {}", title, e.getStatusCode(), e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isExactMatch(AnilibriaReleaseDto release, String targetTitle) {
        if (release.name() == null) {
            return false;
        }
        String main = release.name().main();
        String english = release.name().english();
        return (main != null && main.equalsIgnoreCase(targetTitle)) || (english != null && english.equalsIgnoreCase(targetTitle));
    }
}
