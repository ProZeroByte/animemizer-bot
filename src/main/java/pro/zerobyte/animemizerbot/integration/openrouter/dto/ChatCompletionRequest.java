package pro.zerobyte.animemizerbot.integration.openrouter.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ChatCompletionRequest(
        String model,
        List<Message> messages,
        Double temperature,
        Integer maxTokens
) {
    public record Message(String role, String content) {
    }
}
