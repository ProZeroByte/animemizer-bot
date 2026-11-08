package pro.zerobyte.animemizerbot.integration.tavily.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TavilySearchResponse(
        String query,
        List<TavilySearchResult> results
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TavilySearchResult(
            String title,
            String url,
            String content,
            Double score
    ) {
    }
}
