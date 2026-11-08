package pro.zerobyte.animemizerbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import pro.zerobyte.animemizerbot.config.props.AnimeUrlProperties;
import pro.zerobyte.animemizerbot.config.props.TavilyProperties;
import pro.zerobyte.animemizerbot.core.anime.AnimeSource;
import pro.zerobyte.animemizerbot.integration.shikimori.ShikimoriClient;
import pro.zerobyte.animemizerbot.integration.tavily.TavilyClient;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.USER_AGENT;

@Configuration
public class HttpClientConfig {
    private static final Logger log = LoggerFactory.getLogger(HttpClientConfig.class);

    @Bean
    public RestClient shikimoriRestClient(AnimeUrlProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        return RestClient.builder()
                .baseUrl(properties.getUrlFor(AnimeSource.SHIKIMORI))
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader(USER_AGENT, "Animemizer-bot")
                .defaultHeader(ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor((request, body, execution) -> {
                    ClientHttpResponse response = execution.execute(request, body);
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        log.warn("Request failed with status: {}", response.getStatusCode());
                    }
                    return response;
                })
                .build();
    }

    @Bean
    public ShikimoriClient shikimoriClient(RestClient shikimoriRestClient) {
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(shikimoriRestClient))
                .build()
                .createClient(ShikimoriClient.class);
    }

    @Bean
    public RestClient tavilyRestClient(TavilyProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        return RestClient.builder()
                .baseUrl(properties.resolvedBaseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader(USER_AGENT, "Animemizer-bot")
                .defaultHeader(ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .build();
    }

    @Bean
    public TavilyClient tavilyClient(RestClient tavilyRestClient) {
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(tavilyRestClient))
                .build()
                .createClient(TavilyClient.class);
    }
}
