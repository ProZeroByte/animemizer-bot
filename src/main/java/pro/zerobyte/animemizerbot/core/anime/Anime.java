package pro.zerobyte.animemizerbot.core.anime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Table("anime")
public record Anime(
        @Id Long id,
        String publicId,
        String prequelPublicId,
        String sequelPublicId,
        String name,
        String russianName,
        String englishName,
        String description,
        String posterUrl,
        String kind,
        Boolean isReleased,
        Boolean isAnons,
        Boolean isOngoing,
        Integer episodes,
        Integer episodesAired,
        LocalDate airedOn,
        LocalDate releasedOn,
        LocalDateTime updatedAt,
        LocalDateTime nextEpisodeAt,
        String watchUrl
) {
    /**
     * Computes the released status based on airing state and episode counts.
     * An anime is considered released if it's not ongoing or if all episodes have aired.
     */
    public static boolean computeIsReleased(Boolean ongoing, Integer episodes, Integer episodesAired) {
        return !Boolean.TRUE.equals(ongoing) || Objects.equals(episodes, episodesAired);
    }

    public Anime withId(Long newId) {
        return new Anime(newId, publicId, prequelPublicId, sequelPublicId, name, russianName,
                englishName, description, posterUrl, kind, isReleased, isAnons, isOngoing,
                episodes, episodesAired, airedOn, releasedOn, updatedAt, nextEpisodeAt, watchUrl);
    }

    public Anime withDescription(String newDescription) {
        return new Anime(id, publicId, prequelPublicId, sequelPublicId, name, russianName,
                englishName, newDescription, posterUrl, kind, isReleased, isAnons, isOngoing,
                episodes, episodesAired, airedOn, releasedOn, updatedAt, nextEpisodeAt, watchUrl);
    }

    public Anime withWatchUrl(String newWatchUrl) {
        return new Anime(id, publicId, prequelPublicId, sequelPublicId, name, russianName,
                englishName, description, posterUrl, kind, isReleased, isAnons, isOngoing,
                episodes, episodesAired, airedOn, releasedOn, updatedAt, nextEpisodeAt, newWatchUrl);
    }
}
