package pro.zerobyte.animemizerbot.integration.shikimori;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import pro.zerobyte.animemizerbot.config.props.ShikimoriProperties;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeSource;
import pro.zerobyte.animemizerbot.integration.shikimori.dto.ShikimoriAnimeDto;
import pro.zerobyte.animemizerbot.integration.shikimori.dto.ShikimoriAnimeRelatedDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class ShikimoriMapper {
    private final ShikimoriProperties shikimoriProperties;

    public ShikimoriMapper(ShikimoriProperties shikimoriProperties) {
        this.shikimoriProperties = shikimoriProperties;
    }

    public Anime toEntity(ShikimoriAnimeDto dto, List<ShikimoriAnimeRelatedDto> relatedAnimes) {
        return new Anime(
                null,
                AnimeSource.SHIKIMORI.createPublicId(dto.id()),
                extractRelatedId(relatedAnimes, "Prequel"),
                extractRelatedId(relatedAnimes, "Sequel"),
                dto.name(),
                dto.russian(),
                CollectionUtils.firstElement(dto.english()),
                dto.description(),
                shikimoriProperties.resolvedBaseUrl() + dto.image().original(),
                dto.kind(),
                Anime.computeIsReleased(dto.ongoing(), dto.episodes(), dto.episodesAired()),
                dto.anons(),
                dto.ongoing(),
                dto.episodes(),
                dto.episodesAired(),
                dto.airedOn(),
                dto.releasedOn(),
                toLocalDateTime(dto.updatedAt()),
                toLocalDateTime(dto.nextEpisodeAt()),
                null
        );
    }

    private String extractRelatedId(List<ShikimoriAnimeRelatedDto> relatedAnimes, String relation) {
        if (relatedAnimes == null || relatedAnimes.isEmpty()) {
            return null;
        }
        return relatedAnimes.stream()
                .filter(r -> relation.equals(r.relation()))
                .filter(r -> r.anime() != null)
                .map(r -> AnimeSource.SHIKIMORI.createPublicId(r.anime().id()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Converts various date/time types to LocalDateTime for domain entity storage.
     * Handles OffsetDateTime (from Shikimori API timestamps with timezone) and LocalDate.
     */
    private LocalDateTime toLocalDateTime(Object dateTime) {
        return switch (dateTime) {
            case null -> null;
            case LocalDateTime ldt -> ldt;
            case OffsetDateTime odt -> odt.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            case LocalDate ld -> ld.atStartOfDay();
            default -> throw new IllegalArgumentException("Unsupported date/time type: " + dateTime.getClass());
        };
    }
}