package pro.zerobyte.animemizerbot.integration.shikimori.dto;


import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ShikimoriAnimeDto(
        Long id,
        String name,
        String russian,
        ShikimoriImageDto image,
        String kind,
        String status,
        Integer episodes,
        Integer episodesAired,
        LocalDate airedOn,
        LocalDate releasedOn,
        List<String> english,
        List<String> japanese,
        String description,
        String descriptionHtml,
        String franchise,
        Boolean anons,
        Boolean ongoing,
        Integer threadId,
        Integer topicId,
        OffsetDateTime updatedAt,
        OffsetDateTime nextEpisodeAt
) {
    public record ShikimoriImageDto(
            String original,
            String preview,
            String x96,
            String x48
    ) {}
}
