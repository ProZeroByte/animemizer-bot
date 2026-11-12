package pro.zerobyte.animemizerbot.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.web.client.RestClient;
import pro.zerobyte.animemizerbot.config.props.ShikimoriProperties;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class HttpClientConfigTest {

    @Test
    void should_useConfiguredBaseUrl_when_creatingShikimoriRestClient() throws IOException {
        // Given
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> requestedPath = new AtomicReference<>();
        server.createContext("/", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        try {
            String configuredBaseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            ShikimoriProperties properties = new Binder(new MapConfigurationPropertySource(
                            Map.of("shikimori.base-url", configuredBaseUrl)))
                    .bind("shikimori", Bindable.of(ShikimoriProperties.class))
                    .orElseThrow(IllegalStateException::new);
            RestClient restClient = new HttpClientConfig().shikimoriRestClient(properties);

            // When
            restClient.get().uri("/probe").retrieve().toBodilessEntity();

            // Then
            assertThat(requestedPath.get()).isEqualTo("/probe");
        } finally {
            server.stop(0);
        }
    }
}