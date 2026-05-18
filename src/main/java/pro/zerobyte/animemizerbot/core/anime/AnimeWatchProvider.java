package pro.zerobyte.animemizerbot.core.anime;

import java.util.Optional;

public interface AnimeWatchProvider {

    AnimeSource getSource();

    Optional<String> findExternalId(Anime anime);
}
