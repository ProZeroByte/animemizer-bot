package pro.zerobyte.animemizerbot.core.anime;

import java.util.Optional;

/**
 * Resolves a single direct watch URL for an anime.
 */
public interface WatchLinkResolver {

    Optional<String> resolveWatchUrl(Anime anime);
}
