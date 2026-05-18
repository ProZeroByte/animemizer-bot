package pro.zerobyte.animemizerbot.integration.anilibria;

import pro.zerobyte.animemizerbot.integration.anilibria.dto.AnilibriaReleaseDto;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import java.util.List;

@HttpExchange
public interface AnilibriaClient {

    @GetExchange("/api/v1/app/search/releases")
    List<AnilibriaReleaseDto> searchReleases(
            @RequestParam("query") String query,
            @RequestParam("include") String include
    );
}
