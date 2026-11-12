package pro.zerobyte.animemizerbot.integration.kinopoisk.dto;

import java.util.List;

/**
 * Response of {@code GET /api/v2.1/films/search-by-keyword} of Kinopoisk API Unofficial.
 * The API returns camelCase keys, so no naming strategy is applied.
 */
public record KinopoiskSearchResponse(
        String keyword,
        Integer pagesCount,
        Integer searchFilmsCountResult,
        List<KinopoiskFilm> films
) {
    public record KinopoiskFilm(
            Long filmId,
            String nameRu,
            String nameEn,
            String type,
            String year,
            String description,
            String filmLength,
            String rating,
            Integer ratingVoteCount,
            String posterUrl,
            String posterUrlPreview
    ) {
    }
}
