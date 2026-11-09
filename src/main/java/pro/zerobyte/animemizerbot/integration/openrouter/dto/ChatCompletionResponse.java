package pro.zerobyte.animemizerbot.integration.openrouter.dto;

import java.util.List;

public record ChatCompletionResponse(List<Choice> choices) {
    public record Choice(Message message) {
    }

    public record Message(String content) {
    }
}
