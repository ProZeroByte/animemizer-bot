package pro.zerobyte.animemizerbot.dto.shikimori;

import pro.zerobyte.animemizerbot.config.JacksonConfig;
import pro.zerobyte.animemizerbot.integration.shikimori.dto.ShikimoriAnimeDto;
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
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ShikimoriAnimeDtoTest {

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
            File file = ResourceUtils.getFile("classpath:json/shikimori/shikimori_anime_dto.json");
            String json = Files.readString(file.toPath());

            // When
            ShikimoriAnimeDto dto = jsonService.fromJson(json, ShikimoriAnimeDto.class);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(55644);
            assertThat(dto.name()).isEqualTo("Dr. Stone: New World Part 2");
            assertThat(dto.russian()).isEqualTo("\u0414\u043e\u043a\u0442\u043e\u0440 \u0421\u0442\u043e\u0443\u043d: \u041d\u043e\u0432\u044b\u0439 \u043c\u0438\u0440. \u0427\u0430\u0441\u0442\u044c 2");
            assertThat(dto.kind()).isEqualTo("tv");
            assertThat(dto.status()).isEqualTo("released");
            assertThat(dto.episodes()).isEqualTo(11);
            assertThat(dto.episodesAired()).isEqualTo(10);
            assertThat(dto.image()).isNotNull();
            assertThat(dto.image().original()).isEqualTo("/system/animes/original/55644.jpg?1718586054");
            assertThat(dto.image().preview()).isEqualTo("/system/animes/preview/55644.jpg?1718586054");
            assertThat(dto.image().x96()).isEqualTo("/system/animes/x96/55644.jpg?1718586054");
            assertThat(dto.image().x48()).isEqualTo("/system/animes/x48/55644.jpg?1718586054");
            assertThat(dto.english()).isNotNull();
            assertThat(dto.english()).hasSize(1);
            assertThat(dto.english().getFirst()).isEqualTo("Dr. Stone: New World Part 2");
            assertThat(dto.japanese()).isNotNull();
            assertThat(dto.japanese()).hasSize(1);
            assertThat(dto.japanese().getFirst()).isEqualTo("Dr.STONE NEW WORLD");
            assertThat(dto.airedOn()).isEqualTo(LocalDate.parse("2023-10-12"));
            assertThat(dto.releasedOn()).isEqualTo(LocalDate.parse("2023-12-21"));
            assertThat(dto.description()).isNull();
            assertThat(dto.descriptionHtml()).isEqualTo("<div class=\"b-text_with_paragraphs\"></div>");
            assertThat(dto.franchise()).isEqualTo("dr_stone");
            assertThat(dto.anons()).isFalse();
            assertThat(dto.ongoing()).isFalse();
            assertThat(dto.threadId()).isEqualTo(512207);
            assertThat(dto.topicId()).isEqualTo(512207);
            assertThat(dto.updatedAt()).isEqualTo(OffsetDateTime.parse("2026-05-19T08:38:42.344+03:00"));
            assertThat(dto.nextEpisodeAt()).isNull();
        }
    }
}
