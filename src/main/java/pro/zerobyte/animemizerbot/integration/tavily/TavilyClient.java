package pro.zerobyte.animemizerbot.integration.tavily;

import pro.zerobyte.animemizerbot.integration.tavily.dto.TavilySearchRequest;
import pro.zerobyte.animemizerbot.integration.tavily.dto.TavilySearchResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface TavilyClient {

    @PostExchange("/search")
    TavilySearchResponse search(@RequestBody TavilySearchRequest request);
}
