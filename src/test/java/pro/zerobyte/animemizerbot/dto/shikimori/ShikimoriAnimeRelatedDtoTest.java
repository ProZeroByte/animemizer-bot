package pro.zerobyte.animemizerbot.dto.shikimori;

import tools.jackson.core.type.TypeReference;
import pro.zerobyte.animemizerbot.config.JacksonConfig;
import pro.zerobyte.animemizerbot.integration.shikimori.dto.ShikimoriAnimeRelatedDto;
import pro.zerobyte.animemizerbot.core.service.JsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShikimoriAnimeRelatedDtoTest {

    private JsonService jsonService;

    @BeforeEach
    void setUp() {
        JacksonConfig config = new JacksonConfig();
        jsonService = new JsonService(config.jsonMapper());
    }

    @Nested
    @DisplayName("deserialization")
    class Deserialization {

        @Test
        void should_deserializeCorrectly_when_validJsonProvided() throws IOException {
            // Given
            File file = ResourceUtils.getFile("classpath:json/shikimori/shikimori_anime_related_dto.json");
            String json = Files.readString(file.toPath());

            // When
            List<ShikimoriAnimeRelatedDto> dtoList = jsonService.fromJson(json, new TypeReference<>() {});

            // Then
            assertThat(dtoList).isNotNull();
            assertThat(dtoList).isNotEmpty();

            ShikimoriAnimeRelatedDto firstDto = dtoList.getFirst();
            assertThat(firstDto.relation()).isEqualTo("Adaptation");
            assertThat(firstDto.relationRussian()).isEqualTo("\u0410\u0434\u0430\u043f\u0442\u0430\u0446\u0438\u044f");
            assertThat(firstDto.anime()).isNull();
            assertThat(firstDto.manga()).isNotNull();
            assertThat(firstDto.manga().id()).isEqualTo(103897);
            assertThat(firstDto.manga().name()).isEqualTo("Dr. Stone");
            assertThat(firstDto.manga().russian()).isEqualTo("\u0414\u043e\u043a\u0442\u043e\u0440 \u0421\u0442\u043e\u0443\u043d");
            assertThat(firstDto.manga().image()).isNotNull();
            assertThat(firstDto.manga().image().original()).isEqualTo("/system/mangas/original/103897.jpg?1712482541");
            assertThat(firstDto.manga().image().preview()).isEqualTo("/system/mangas/preview/103897.jpg?1712482541");
            assertThat(firstDto.manga().image().x96()).isEqualTo("/system/mangas/x96/103897.jpg?1712482541");
            assertThat(firstDto.manga().image().x48()).isEqualTo("/system/mangas/x48/103897.jpg?1712482541");
            assertThat(firstDto.manga().kind()).isEqualTo("manga");
            assertThat(firstDto.manga().status()).isEqualTo("released");
            assertThat(firstDto.manga().airedOn()).isEqualTo(LocalDate.parse("2017-03-06"));
            assertThat(firstDto.manga().releasedOn()).isEqualTo(LocalDate.parse("2023-12-25"));

            ShikimoriAnimeRelatedDto lastDto = dtoList.getLast();
            assertThat(lastDto.relation()).isEqualTo("Sequel");
            assertThat(lastDto.relationRussian()).isEqualTo("\u041f\u0440\u043e\u0434\u043e\u043b\u0436\u0435\u043d\u0438\u0435");
            assertThat(lastDto.anime()).isNotNull();
            assertThat(lastDto.manga()).isNull();
            assertThat(lastDto.anime().id()).isEqualTo(57592);
            assertThat(lastDto.anime().name()).isEqualTo("Dr. Stone: Science Future");
            assertThat(lastDto.anime().russian()).isEqualTo("\u0414\u043e\u043a\u0442\u043e\u0440 \u0421\u0442\u043e\u0443\u043d: \u041d\u0430\u0443\u0447\u043d\u043e\u0435 \u0431\u0443\u0434\u0443\u0449\u0435\u0435");
            assertThat(lastDto.anime().image()).isNotNull();
            assertThat(lastDto.anime().image().original()).isEqualTo("/system/animes/original/57592.jpg?1711049777");
            assertThat(lastDto.anime().image().preview()).isEqualTo("/system/animes/preview/57592.jpg?1711049777");
            assertThat(lastDto.anime().image().x96()).isEqualTo("/system/animes/x96/57592.jpg?1711049777");
            assertThat(lastDto.anime().image().x48()).isEqualTo("/system/animes/x48/57592.jpg?1711049777");
            assertThat(lastDto.anime().kind()).isEqualTo("tv");
            assertThat(lastDto.anime().status()).isEqualTo("released");
            assertThat(lastDto.anime().episodes()).isEqualTo(12);
            assertThat(lastDto.anime().episodesAired()).isEqualTo(12);
            assertThat(lastDto.anime().airedOn()).isEqualTo(LocalDate.parse("2025-01-09"));
            assertThat(lastDto.anime().releasedOn()).isEqualTo(LocalDate.parse("2025-03-27"));
        }
    }
}
