package pro.zerobyte.animemizerbot.integration.shikimori;

import pro.zerobyte.animemizerbot.integration.shikimori.dto.ShikimoriAnimeDto;
import pro.zerobyte.animemizerbot.integration.shikimori.dto.ShikimoriAnimeRelatedDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange
public interface ShikimoriClient {

    @GetExchange("/api/animes")
    List<ShikimoriAnimeDto> searchAnime(@RequestParam("search") String title, @RequestParam("limit") int limit);

    @GetExchange("/api/animes/{id}")
    ShikimoriAnimeDto getAnimeDetails(@PathVariable long id);

    @GetExchange("/api/animes/{id}/related")
    List<ShikimoriAnimeRelatedDto> getRelatedAnimes(@PathVariable long id);
}
