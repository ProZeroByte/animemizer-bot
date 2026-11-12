package pro.zerobyte.animemizerbot.integration.shikimori;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import pro.zerobyte.animemizerbot.config.JacksonConfig;
import pro.zerobyte.animemizerbot.config.props.ShikimoriProperties;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.integration.shikimori.dto.ShikimoriAnimeDto;
import pro.zerobyte.animemizerbot.integration.shikimori.dto.ShikimoriAnimeRelatedDto;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShikimoriMapperTest {

    private static final String SHIKIMORI_BASE_URL = "https://shikimori.one";

    private ShikimoriMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mapper = new ShikimoriMapper(new ShikimoriProperties(SHIKIMORI_BASE_URL));
        JacksonConfig config = new JacksonConfig();
        objectMapper = config.jsonMapper();
    }

    private ShikimoriAnimeDto loadAnimeDto() throws IOException {
        try (InputStream is = new ClassPathResource("json/shikimori/shikimori_anime_dto.json").getInputStream()) {
            return objectMapper.readValue(is, ShikimoriAnimeDto.class);
        }
    }

    private List<ShikimoriAnimeRelatedDto> loadRelatedDtos() throws IOException {
        try (InputStream is = new ClassPathResource("json/shikimori/shikimori_anime_related_dto.json").getInputStream()) {
            return objectMapper.readerForListOf(ShikimoriAnimeRelatedDto.class).readValue(is);
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        void should_mapAllFields_when_validDtoProvided() throws IOException {
            // Given
            ShikimoriAnimeDto dto = loadAnimeDto();
            List<ShikimoriAnimeRelatedDto> relatedDtos = loadRelatedDtos();

            // When
            Anime anime = mapper.toEntity(dto, relatedDtos);

            // Then
            assertThat(anime).isNotNull();
            assertThat(anime.name()).isEqualTo("Dr. Stone: New World Part 2");
            assertThat(anime.russianName()).isEqualTo("\u0414\u043e\u043a\u0442\u043e\u0440 \u0421\u0442\u043e\u0443\u043d: \u041d\u043e\u0432\u044b\u0439 \u043c\u0438\u0440. \u0427\u0430\u0441\u0442\u044c 2");
            assertThat(anime.posterUrl()).contains("original");
            assertThat(anime.posterUrl()).startsWith(SHIKIMORI_BASE_URL);
            assertThat(anime.publicId()).startsWith("shikimori_");
            assertThat(anime.publicId()).isEqualTo("shikimori_55644");
            assertThat(anime.episodes()).isEqualTo(11);
            assertThat(anime.episodesAired()).isEqualTo(10);
            assertThat(anime.kind()).isEqualTo("tv");
            assertThat(anime.watchUrl()).isNull();
        }

        @Test
        void should_extractPrequel_when_relatedAnimeExists() throws IOException {
            // Given
            ShikimoriAnimeDto dto = loadAnimeDto();
            List<ShikimoriAnimeRelatedDto> relatedDtos = loadRelatedDtos();

            // When
            Anime anime = mapper.toEntity(dto, relatedDtos);

            // Then
            assertThat(anime.prequelPublicId()).isEqualTo("shikimori_48549");
        }

        @Test
        void should_extractSequel_when_relatedAnimeExists() throws IOException {
            // Given
            ShikimoriAnimeDto dto = loadAnimeDto();
            List<ShikimoriAnimeRelatedDto> relatedDtos = loadRelatedDtos();

            // When
            Anime anime = mapper.toEntity(dto, relatedDtos);

            // Then
            assertThat(anime.sequelPublicId()).isEqualTo("shikimori_57592");
        }

        @Test
        void should_returnNullPrequel_when_noRelatedAnimes() throws IOException {
            // Given
            ShikimoriAnimeDto dto = loadAnimeDto();

            // When
            Anime anime = mapper.toEntity(dto, Collections.emptyList());

            // Then
            assertThat(anime.prequelPublicId()).isNull();
            assertThat(anime.sequelPublicId()).isNull();
        }
    }
}
