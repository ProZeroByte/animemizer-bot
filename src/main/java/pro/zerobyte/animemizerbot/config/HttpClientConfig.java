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
import pro.zerobyte.animemizerbot.config.props.AnilibertyProperties;
import pro.zerobyte.animemizerbot.config.props.KinopoiskProperties;
import pro.zerobyte.animemizerbot.config.props.OpenRouterProperties;
import pro.zerobyte.animemizerbot.config.props.ShikimoriProperties;
import pro.zerobyte.animemizerbot.integration.aniliberty.AnilibertyClient;
import pro.zerobyte.animemizerbot.integration.kinopoisk.KinopoiskClient;
import pro.zerobyte.animemizerbot.integration.openrouter.OpenRouterClient;
import pro.zerobyte.animemizerbot.integration.shikimori.ShikimoriClient;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.USER_AGENT;

@Configuration
public class HttpClientConfig {
    private static final Logger log = LoggerFactory.getLogger(HttpClientConfig.class);

    @Bean
    public RestClient shikimoriRestClient(ShikimoriProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        return RestClient.builder()
                .baseUrl(properties.resolvedBaseUrl())
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
    public RestClient kinopoiskRestClient(KinopoiskProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        return RestClient.builder()
                .baseUrl(properties.resolvedBaseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader(USER_AGENT, "Animemizer-bot")
                .defaultHeader(ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-API-KEY", properties.apiKey())
                .requestInterceptor((request, body, execution) -> {
                    ClientHttpResponse response = execution.execute(request, body);
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        log.warn("Kinopoisk request {} failed with status: {}", request.getURI(), response.getStatusCode());
                    }
                    return response;
                })
                .build();
    }

    @Bean
    public KinopoiskClient kinopoiskClient(RestClient kinopoiskRestClient) {
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(kinopoiskRestClient))
                .build()
                .createClient(KinopoiskClient.class);
    }

    @Bean
    public RestClient anilibertyRestClient(AnilibertyProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        return RestClient.builder()
                .baseUrl(properties.resolvedBaseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader(USER_AGENT, "Animemizer-bot")
                .defaultHeader(ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor((request, body, execution) -> {
                    ClientHttpResponse response = execution.execute(request, body);
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        log.warn("AniLiberty request {} failed with status: {}", request.getURI(), response.getStatusCode());
                    }
                    return response;
                })
                .build();
    }

    @Bean
    public AnilibertyClient anilibertyClient(RestClient anilibertyRestClient) {
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(anilibertyRestClient))
                .build()
                .createClient(AnilibertyClient.class);
    }

    @Bean
    public RestClient openRouterRestClient(OpenRouterProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        return RestClient.builder()
                .baseUrl(properties.resolvedBaseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader(USER_AGENT, "Animemizer-bot")
                .defaultHeader(ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .requestInterceptor((request, body, execution) -> {
                    ClientHttpResponse response = execution.execute(request, body);
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        log.warn("OpenRouter request {} failed with status: {}", request.getURI(), response.getStatusCode());
                    }
                    return response;
                })
                .build();
    }

    @Bean
    public OpenRouterClient openRouterClient(RestClient openRouterRestClient) {
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(openRouterRestClient))
                .build()
                .createClient(OpenRouterClient.class);
    }
}
