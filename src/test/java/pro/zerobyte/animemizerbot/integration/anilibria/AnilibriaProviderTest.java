package pro.zerobyte.animemizerbot.integration.anilibria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeSource;
import pro.zerobyte.animemizerbot.integration.anilibria.dto.AnilibriaReleaseDto;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnilibriaProviderTest {

    @Mock
    private AnilibriaClient anilibriaClient;

    @InjectMocks
    private AnilibriaProvider anilibriaProvider;

    private Anime newAnime(String name) {
        return new Anime(1L, "shikimori_1", null, null, name, "Русское", null,
                null, null, null, false, false, false, null, null,
                null, null, null, null, null);
    }

    @Nested
    @DisplayName("getSource")
    class GetSource {

        @Test
        void should_returnAnilibria() {
            // When
            AnimeSource source = anilibriaProvider.getSource();

            // Then
            assertThat(source).isEqualTo(AnimeSource.ANILIBRIA);
        }
    }

    @Nested
    @DisplayName("findExternalId")
    class FindExternalId {

        @Test
        void should_returnExternalId_when_exactTitleMatch() {
            // Given
            Anime anime = newAnime("Naruto");
            AnilibriaReleaseDto.AnilibriaNameDto nameDto = new AnilibriaReleaseDto.AnilibriaNameDto("Naruto", "Naruto Ship");
            AnilibriaReleaseDto release = new AnilibriaReleaseDto(42L, nameDto);
            when(anilibriaClient.searchReleases("Naruto", "id,name.main,name.english")).thenReturn(List.of(release));

            // When
            Optional<String> result = anilibriaProvider.findExternalId(anime);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("anime/releases/release/42");
        }

        @Test
        void should_returnExternalId_when_englishTitleMatch() {
            // Given
            Anime anime = newAnime("Naruto");
            AnilibriaReleaseDto.AnilibriaNameDto nameDto = new AnilibriaReleaseDto.AnilibriaNameDto("Наруто", "Naruto");
            AnilibriaReleaseDto release = new AnilibriaReleaseDto(55L, nameDto);
            when(anilibriaClient.searchReleases("Naruto", "id,name.main,name.english")).thenReturn(List.of(release));

            // When
            Optional<String> result = anilibriaProvider.findExternalId(anime);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("anime/releases/release/55");
        }

        @Test
        void should_returnEmpty_when_noMatch() {
            // Given
            Anime anime = newAnime("Naruto");
            AnilibriaReleaseDto.AnilibriaNameDto nameDto = new AnilibriaReleaseDto.AnilibriaNameDto("Bleach", "Bleach");
            AnilibriaReleaseDto release = new AnilibriaReleaseDto(10L, nameDto);
            when(anilibriaClient.searchReleases("Naruto", "id,name.main,name.english")).thenReturn(List.of(release));

            // When
            Optional<String> result = anilibriaProvider.findExternalId(anime);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_nullTitle() {
            // Given
            Anime anime = newAnime(null);

            // When
            Optional<String> result = anilibriaProvider.findExternalId(anime);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_blankTitle() {
            // Given
            Anime anime = newAnime("   ");

            // When
            Optional<String> result = anilibriaProvider.findExternalId(anime);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_noReleases() {
            // Given
            Anime anime = newAnime("Naruto");
            when(anilibriaClient.searchReleases("Naruto", "id,name.main,name.english")).thenReturn(List.of());

            // When
            Optional<String> result = anilibriaProvider.findExternalId(anime);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_apiException() {
            // Given
            Anime anime = newAnime("Naruto");
            when(anilibriaClient.searchReleases(anyString(), anyString()))
                    .thenThrow(HttpClientErrorException.create(
                            org.springframework.http.HttpStatus.NOT_FOUND,
                            "Not Found",
                            org.springframework.http.HttpHeaders.EMPTY,
                            new byte[0],
                            null));

            // When
            Optional<String> result = anilibriaProvider.findExternalId(anime);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
