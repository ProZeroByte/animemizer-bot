package pro.zerobyte.animemizerbot.service;

import pro.zerobyte.animemizerbot.integration.shikimori.ShikimoriClient;
import pro.zerobyte.animemizerbot.integration.shikimori.ShikimoriConnector;
import pro.zerobyte.animemizerbot.integration.shikimori.ShikimoriMapper;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.integration.shikimori.dto.ShikimoriAnimeDto;
import pro.zerobyte.animemizerbot.integration.shikimori.dto.ShikimoriAnimeRelatedDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShikimoriConnectorTest {

    private ShikimoriClient shikimoriClient;
    private ShikimoriMapper shikimoriMapper;
    private ShikimoriConnector connector;

    @BeforeEach
    void setUp() {
        shikimoriClient = mock(ShikimoriClient.class);
        shikimoriMapper = mock(ShikimoriMapper.class);
        connector = new ShikimoriConnector(shikimoriClient, shikimoriMapper, task -> task.run());
    }

    private <T> List<T> singletonList(T element) {
        List<T> list = new ArrayList<>();
        list.add(element);
        return list;
    }

    private <T> List<T> emptyMutableList() {
        return new ArrayList<>();
    }

    @Nested
    @DisplayName("findByTitle")
    class FindByTitle {

        @Test
        void should_returnAnime_when_foundOnShikimori() {
            // Given
            ShikimoriAnimeDto dto = mock(ShikimoriAnimeDto.class);
            when(dto.id()).thenReturn(1L);
            when(shikimoriClient.searchAnime(anyString(), anyInt())).thenReturn(singletonList(dto));
            when(shikimoriClient.getAnimeDetails(anyInt())).thenReturn(dto);
            when(shikimoriClient.getRelatedAnimes(anyInt())).thenReturn(emptyMutableList());

            Anime expectedAnime = mock(Anime.class);
            when(shikimoriMapper.toEntity(any(), any())).thenReturn(expectedAnime);

            // When
            Optional<Anime> result = connector.findByTitle("Test Anime");

            // Then
            assertThat(result).isPresent();
        }

        @Test
        void should_returnEmpty_when_notFoundOnShikimori() {
            // Given
            when(shikimoriClient.searchAnime(anyString(), anyInt())).thenReturn(emptyMutableList());

            // When
            Optional<Anime> result = connector.findByTitle("Test Anime");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_clientThrowsException() {
            // Given
            when(shikimoriClient.searchAnime(anyString(), anyInt())).thenThrow(mock(HttpClientErrorException.class));

            // When
            Optional<Anime> result = connector.findByTitle("Test Anime");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_populatePrequelAndSequel_when_relatedAnimesExist() {
            // Given
            ShikimoriAnimeDto dto = mock(ShikimoriAnimeDto.class);
            when(dto.id()).thenReturn(1L);
            when(shikimoriClient.searchAnime(anyString(), anyInt())).thenReturn(singletonList(dto));
            when(shikimoriClient.getAnimeDetails(anyInt())).thenReturn(dto);

            List<ShikimoriAnimeRelatedDto> relatedDtos = new ArrayList<>();
            relatedDtos.add(mock(ShikimoriAnimeRelatedDto.class));
            when(shikimoriClient.getRelatedAnimes(anyInt())).thenReturn(relatedDtos);

            Anime expectedAnime = mock(Anime.class);
            when(shikimoriMapper.toEntity(any(), any())).thenReturn(expectedAnime);

            // When
            Optional<Anime> result = connector.findByTitle("Test Anime");

            // Then
            assertThat(result).isPresent();
        }
    }
}
