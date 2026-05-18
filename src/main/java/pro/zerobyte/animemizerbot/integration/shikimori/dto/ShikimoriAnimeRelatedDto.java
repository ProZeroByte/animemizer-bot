package pro.zerobyte.animemizerbot.integration.shikimori.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ShikimoriAnimeRelatedDto(
        String relation,
        String relationRussian,
        ShikimoriAnimeDto anime,
        ShikimoriAnimeDto manga
) {}
