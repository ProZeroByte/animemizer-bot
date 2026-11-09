package pro.zerobyte.animemizerbot.integration.tavily.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TavilySearchRequest(
        String query,
        String searchDepth,
        Integer maxResults,
        List<String> includeDomains,
        String includeDomainsMode,
        String country,
        String language
) {
}
