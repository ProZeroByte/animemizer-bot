package pro.zerobyte.animemizerbot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeLink;
import pro.zerobyte.animemizerbot.core.anime.AnimeRepository;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import pro.zerobyte.animemizerbot.core.anime.AnimeSource;
import pro.zerobyte.animemizerbot.core.anime.AnimeWatchProvider;
import pro.zerobyte.animemizerbot.integration.shikimori.ShikimoriConnector;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimeServiceTest {

    private static final Long ANIME_ID = 200L;
    private static final String PUBLIC_ID = "shikimori_200";
    private static final String ANIME_NAME = "Test Anime";
    private static final String RUSSIAN_NAME = "\u0422\u0435\u0441\u0442\u043e\u0432\u043e\u0435 \u0430\u043d\u0438\u043c\u0435";

    @Mock
    private AnimeRepository repository;

    @Mock
    private ShikimoriConnector shikimoriConnector;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private List<AnimeWatchProvider> activeProviders;
    private AnimeService animeService;

    @BeforeEach
    void setUp() {
        activeProviders = Collections.emptyList();
        when(cacheManager.getCache("providerCheckFailures")).thenReturn(cache);
        animeService = new AnimeService(repository, shikimoriConnector, activeProviders, cacheManager);
    }

    private Anime newAnime(Long id, String publicId, String name, String russianName) {
        return new Anime(id, publicId, null, null, name, russianName, null,
                null, null, null, false, false, false, 12, null, null, null, null, null, null);
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        void should_returnAnime_when_foundInRepository() {
            // Given
            Anime anime = newAnime(ANIME_ID, PUBLIC_ID, ANIME_NAME, RUSSIAN_NAME);
            when(repository.findById(ANIME_ID)).thenReturn(Optional.of(anime));

            // When
            Optional<Anime> result = animeService.findById(ANIME_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo(ANIME_NAME);
        }

        @Test
        void should_callEnrichment_when_animeFound() {
            // Given - anime with no links, and a provider that needs enrichment
            Anime anime = newAnime(ANIME_ID, PUBLIC_ID, ANIME_NAME, RUSSIAN_NAME);
            when(repository.findById(ANIME_ID)).thenReturn(Optional.of(anime));

            AnimeWatchProvider provider = mock(AnimeWatchProvider.class);
            when(provider.getSource()).thenReturn(AnimeSource.ANILIBRIA);
            when(provider.findExternalId(anime)).thenReturn(Optional.empty());

            List<AnimeWatchProvider> providers = List.of(provider);
            when(cacheManager.getCache("providerCheckFailures")).thenReturn(cache);
            AnimeService serviceWithProviders = new AnimeService(repository, shikimoriConnector, providers, cacheManager);

            // When
            serviceWithProviders.findById(ANIME_ID);

            // Then
            verify(provider).findExternalId(anime);
        }

        @Test
        void should_returnEmpty_when_notFound() {
            // Given
            when(repository.findById(ANIME_ID)).thenReturn(Optional.empty());

            // When
            Optional<Anime> result = animeService.findById(ANIME_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByTitle")
    class FindByTitle {

        @Test
        void should_returnAnime_when_foundByRussianName() {
            // Given
            Anime anime = newAnime(ANIME_ID, PUBLIC_ID, ANIME_NAME, RUSSIAN_NAME);
            when(repository.findByRussianNameIgnoreCase(RUSSIAN_NAME)).thenReturn(Optional.of(anime));

            // When
            Optional<Anime> result = animeService.findByTitle(RUSSIAN_NAME);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().russianName()).isEqualTo(RUSSIAN_NAME);
        }

        @Test
        void should_returnAnime_when_foundByEnglishName() {
            // Given
            Anime anime = newAnime(ANIME_ID, PUBLIC_ID, ANIME_NAME, RUSSIAN_NAME);
            when(repository.findByRussianNameIgnoreCase(ANIME_NAME)).thenReturn(Optional.empty());
            when(repository.findByEnglishNameIgnoreCase(ANIME_NAME)).thenReturn(Optional.of(anime));

            // When
            Optional<Anime> result = animeService.findByTitle(ANIME_NAME);

            // Then
            assertThat(result).isPresent();
        }

        @Test
        void should_returnAnime_when_foundByName() {
            // Given
            Anime anime = newAnime(ANIME_ID, PUBLIC_ID, ANIME_NAME, RUSSIAN_NAME);
            when(repository.findByRussianNameIgnoreCase(ANIME_NAME)).thenReturn(Optional.empty());
            when(repository.findByEnglishNameIgnoreCase(ANIME_NAME)).thenReturn(Optional.empty());
            when(repository.findByNameIgnoreCase(ANIME_NAME)).thenReturn(Optional.of(anime));

            // When
            Optional<Anime> result = animeService.findByTitle(ANIME_NAME);

            // Then
            assertThat(result).isPresent();
        }

        @Test
        void should_fallbackToShikimori_when_notInDatabase() {
            // Given
            Anime anime = newAnime(ANIME_ID, PUBLIC_ID, ANIME_NAME, RUSSIAN_NAME);
            when(repository.findByRussianNameIgnoreCase(ANIME_NAME)).thenReturn(Optional.empty());
            when(repository.findByEnglishNameIgnoreCase(ANIME_NAME)).thenReturn(Optional.empty());
            when(repository.findByNameIgnoreCase(ANIME_NAME)).thenReturn(Optional.empty());
            when(shikimoriConnector.findByTitle(ANIME_NAME)).thenReturn(Optional.of(anime));

            // When
            Optional<Anime> result = animeService.findByTitle(ANIME_NAME);

            // Then
            assertThat(result).isPresent();
            verify(shikimoriConnector).findByTitle(ANIME_NAME);
        }

        @Test
        void should_returnEmpty_when_notFoundAnywhere() {
            // Given
            when(repository.findByRussianNameIgnoreCase(ANIME_NAME)).thenReturn(Optional.empty());
            when(repository.findByEnglishNameIgnoreCase(ANIME_NAME)).thenReturn(Optional.empty());
            when(repository.findByNameIgnoreCase(ANIME_NAME)).thenReturn(Optional.empty());
            when(shikimoriConnector.findByTitle(ANIME_NAME)).thenReturn(Optional.empty());

            // When
            Optional<Anime> result = animeService.findByTitle(ANIME_NAME);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByPublicIdOrFetch")
    class FindByPublicIdOrFetch {

        @Test
        void should_returnAnime_when_foundByPublicId() {
            // Given
            Anime anime = newAnime(ANIME_ID, PUBLIC_ID, ANIME_NAME, RUSSIAN_NAME);
            when(repository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.of(anime));

            // When
            Optional<Anime> result = animeService.findByPublicIdOrFetch(PUBLIC_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().publicId()).isEqualTo(PUBLIC_ID);
        }

        @Test
        void should_returnEmpty_when_nullPublicId() {
            // When
            Optional<Anime> result = animeService.findByPublicIdOrFetch(null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_returnEmpty_when_blankPublicId() {
            // When
            Optional<Anime> result = animeService.findByPublicIdOrFetch("   ");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_fallbackToShikimori_when_notInDatabase() {
            // Given
            Anime anime = newAnime(ANIME_ID, PUBLIC_ID, ANIME_NAME, RUSSIAN_NAME);
            when(repository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.empty());
            when(shikimoriConnector.findById(200L)).thenReturn(Optional.of(anime));

            // When
            Optional<Anime> result = animeService.findByPublicIdOrFetch(PUBLIC_ID);

            // Then
            assertThat(result).isPresent();
            verify(shikimoriConnector).findById(200L);
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        void should_delegateToRepository() {
            // Given
            Anime anime = newAnime(null, PUBLIC_ID, ANIME_NAME, RUSSIAN_NAME);
            Anime savedAnime = newAnime(ANIME_ID, PUBLIC_ID, ANIME_NAME, RUSSIAN_NAME);
            when(repository.save(anime)).thenReturn(savedAnime);

            // When
            Anime result = animeService.save(anime);

            // Then
            assertThat(result.id()).isEqualTo(ANIME_ID);
            verify(repository).save(anime);
        }
    }
}
