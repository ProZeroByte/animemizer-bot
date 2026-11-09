package pro.zerobyte.animemizerbot.integration.openrouter;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import pro.zerobyte.animemizerbot.integration.openrouter.dto.ChatCompletionRequest;
import pro.zerobyte.animemizerbot.integration.openrouter.dto.ChatCompletionResponse;

@HttpExchange
public interface OpenRouterClient {

    @PostExchange("/chat/completions")
    ChatCompletionResponse complete(@RequestBody ChatCompletionRequest request);
}
