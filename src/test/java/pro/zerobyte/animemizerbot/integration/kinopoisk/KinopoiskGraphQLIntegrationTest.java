package pro.zerobyte.animemizerbot.integration.kinopoisk;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "KINOPOISK_API_KEY", matches = "\\S+")
class KinopoiskGraphQLIntegrationTest {

    private static final String API_BASE_URL = "https://kinopoiskapiunofficial.tech";
    private static final String API_KEY_ENVIRONMENT_VARIABLE = "KINOPOISK_API_KEY";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("Получение фильма по идентификатору через Kinopoisk API")
    void should_returnFilmDetails_when_filmIdExists() throws IOException {
        // Given
        String filmUrl = API_BASE_URL + "/api/v2.2/films/301";

        // When
        Connection connection = Jsoup.connect(filmUrl)
                .header("X-API-KEY", System.getenv(API_KEY_ENVIRONMENT_VARIABLE))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .timeout(10_000);
        Connection.Request request = connection.request();
        Connection.Response response = connection.execute();

        // Then
        JsonNode film = parseResponse(response, request);
        assertThat(film.path("kinopoiskId").asInt())
                .as("Ответ должен содержать сведения о фильме 301")
                .isEqualTo(301);
    }

    @Test
    @DisplayName("Поиск фильма по ключевому слову через Kinopoisk API")
    void should_returnSearchResults_when_keywordIsNaruto() throws IOException {
        // Given
        String keyword = URLEncoder.encode("наруто", StandardCharsets.UTF_8);
        String searchUrl = API_BASE_URL + "/api/v2.1/films/search-by-keyword?keyword=" + keyword;

        // When
        Connection connection = Jsoup.connect(searchUrl)
                .header("X-API-KEY", System.getenv(API_KEY_ENVIRONMENT_VARIABLE))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .timeout(10_000);
        Connection.Request request = connection.request();
        Connection.Response response = connection.execute();

        // Then
        JsonNode searchResult = parseResponse(response, request);
        JsonNode films = searchResult.path("films");
        assertThat(films.isArray())
                .as("Ответ поиска должен содержать массив films")
                .isTrue();
        assertThat(films.size())
                .as("По запросу «наруто» должен быть найден хотя бы один фильм")
                .isPositive();
    }

    private JsonNode parseResponse(Connection.Response response, Connection.Request request) throws IOException {
        String responseBody = response.body();
        String requestHeaderNames = String.join(", ", request.headers().keySet());
        assertThat(response.statusCode())
                .as("Kinopoisk API вернул HTTP статус %s. Тело ответа: %s. Запрос: %s %s; заголовки: %s",
                        response.statusCode(), responseBody, request.method(), response.url(), requestHeaderNames)
                .isEqualTo(200);
        assertThat(responseBody).isNotBlank();
        return OBJECT_MAPPER.readTree(responseBody);
    }
}
