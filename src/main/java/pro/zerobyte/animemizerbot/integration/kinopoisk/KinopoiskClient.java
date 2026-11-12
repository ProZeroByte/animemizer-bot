package pro.zerobyte.animemizerbot.integration.kinopoisk;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import pro.zerobyte.animemizerbot.integration.kinopoisk.dto.KinopoiskSearchResponse;

@HttpExchange
public interface KinopoiskClient {

    @GetExchange("/api/v2.1/films/search-by-keyword")
    KinopoiskSearchResponse searchByKeyword(@RequestParam("keyword") String keyword);
}
