package pro.zerobyte.animemizerbot.controller;

import pro.zerobyte.animemizerbot.config.props.AnimeUrlProperties;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeLink;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.anime.AnimeSource;
import pro.zerobyte.animemizerbot.core.anime.AnimeWatchProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/watch")
public class WatchRedirectController {

    private static final Logger log = LoggerFactory.getLogger(WatchRedirectController.class);

    private final AnimeService animeService;
    private final AnimeUrlProperties urlProperties;
    private final Map<AnimeSource, AnimeWatchProvider> providers;

    public WatchRedirectController(AnimeService animeService,
                                   AnimeUrlProperties urlProperties,
                                   List<AnimeWatchProvider> animeWatchProviders) {
        this.animeService = animeService;
        this.urlProperties = urlProperties;
        this.providers = animeWatchProviders.stream().collect(Collectors.toMap(AnimeWatchProvider::getSource, Function.identity()));
    }

    @GetMapping
    public ResponseEntity<Void> redirectToAnime(@RequestParam AnimeSource source, @RequestParam String publicId) {
        AnimeWatchProvider provider = providers.get(source);
        if (provider == null) {
            log.warn("Provider is not configured for source: {}", source);
            return ResponseEntity.notFound().build();
        }

        Optional<Anime> anime = animeService.findByPublicIdOrFetch(publicId);
        if (anime.isEmpty()) {
            log.warn("Anime not found in DB by publicId: {}", publicId);
            return ResponseEntity.notFound().build();
        }

        Optional<String> externalId = anime.flatMap(a -> a.links()
                .stream()
                .filter(link -> link.source() == source)
                .map(AnimeLink::externalId).findFirst()
                .or(() -> fetchAndSaveExternalId(provider, a, source)));

        if (externalId.isEmpty()) {
            log.warn("Anime '{}' not found on site {}", anime.get().russianName(), source);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(urlProperties.getUrlFor(source) + "/" + externalId.get()))
                .build();
    }

    private Optional<String> fetchAndSaveExternalId(AnimeWatchProvider provider, Anime anime, AnimeSource source) {
        log.info("Link not found in DB. Initiating parsing for '{}' on {}", anime.russianName(), source);
        // Note: This enrichment bypasses AnimeService.enrichAndSaveIfRequired() failure caching.
        // If the provider consistently fails, this will retry on every redirect request.
        // Consider routing through AnimeService if this becomes a problem.
        return provider.findExternalId(anime)
                .map(parsedId -> {
                    Set<AnimeLink> links = new HashSet<>(anime.links() != null ? anime.links() : Set.of());
                    links.add(new AnimeLink(null, anime.id(), source, parsedId));
                    animeService.save(anime.withLinks(Set.copyOf(links)));

                    log.info("Successfully parsed and saved new link for source: {}", source);
                    return parsedId;
                });
    }
}
