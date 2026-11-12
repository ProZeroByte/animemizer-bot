package pro.zerobyte.animemizerbot.integration.aniliberty.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * A single release from {@code GET /api/v1/app/search/releases} of AniLiberty.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AnilibertyReleaseDto(
        Long id,
        AnilibertyTypeDto type,
        Integer year,
        AnilibertyNameDto name,
        String alias,
        AnilibertyShikimoriDto shikimori,
        Integer episodesTotal,
        Boolean isOngoing
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AnilibertyTypeDto(String value, String description) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AnilibertyNameDto(String main, String english, String alternative) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AnilibertyShikimoriDto(Long id, String url, Integer votes, Double rating) {
    }
}
