package pro.zerobyte.animemizerbot.integration.aniliberty;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import pro.zerobyte.animemizerbot.integration.aniliberty.dto.AnilibertyReleaseDto;

import java.util.List;

@HttpExchange
public interface AnilibertyClient {

    @GetExchange("/api/v1/app/search/releases")
    List<AnilibertyReleaseDto> searchReleases(@RequestParam("query") String query);
}
