package pro.zerobyte.animemizerbot.core.watchlist;

import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WatchlistService {
    private final WatchlistRepository repository;
    private final AnimeService animeService;

    public WatchlistService(WatchlistRepository repository, AnimeService animeService) {
        this.repository = repository;
        this.animeService = animeService;
    }

    public Optional<Watchlist> getWatchlist(Long telegramUserId, Long animeId) {
        return repository.findByTelegramUserIdAndAnimeId(telegramUserId, animeId);
    }

    public Watchlist incrementEpisode(Long telegramUserId, Long animeId, int maxEpisodes) {
        Watchlist watchlist = repository.findByTelegramUserIdAndAnimeId(telegramUserId, animeId)
                .orElseGet(() -> createNewWatchlist(telegramUserId, animeId));

        int currentEpisode = watchlist.lastWatchedEpisode() != null ? watchlist.lastWatchedEpisode() : 0;
        int max = maxEpisodes > 0 ? maxEpisodes : Integer.MAX_VALUE;

        if (currentEpisode < max) {
            int newEpisode = currentEpisode + 1;
            WatchStatus newStatus = (newEpisode >= max && max != Integer.MAX_VALUE)
                    ? watchlist.status().onAllEpisodesWatched()
                    : watchlist.status();

            Watchlist updated = new Watchlist(
                    watchlist.id(),
                    watchlist.telegramUserId(),
                    watchlist.animeId(),
                    newStatus,
                    newEpisode,
                    watchlist.addedAt(),
                    LocalDateTime.now()
            );
            return repository.save(updated);
        }
        return watchlist;
    }

    public Watchlist decreaseEpisode(Long telegramUserId, Long animeId) {
        Optional<Watchlist> optionalWatchlist = repository.findByTelegramUserIdAndAnimeId(telegramUserId, animeId);

        if (optionalWatchlist.isPresent()) {
            Watchlist watchlist = optionalWatchlist.get();
            int currentEpisode = watchlist.lastWatchedEpisode() != null ? watchlist.lastWatchedEpisode() : 0;

            if (currentEpisode > 0) {
                int newEpisode = currentEpisode - 1;
                WatchStatus newStatus = watchlist.status();

                // Revert COMPLETED status when going below total episodes
                if (newStatus == WatchStatus.COMPLETED) {
                    Optional<Anime> animeOpt = animeService.findById(animeId);
                    if (animeOpt.isPresent()) {
                        Anime anime = animeOpt.get();
                        if (anime.episodes() != null && anime.episodes() > 0 && newEpisode < anime.episodes()) {
                            newStatus = WatchStatus.WATCHING;
                        }
                    }
                }

                Watchlist updated = new Watchlist(
                        watchlist.id(),
                        watchlist.telegramUserId(),
                        watchlist.animeId(),
                        newStatus,
                        newEpisode,
                        watchlist.addedAt(),
                        LocalDateTime.now()
                );
                return repository.save(updated);
            }
            return watchlist;
        }

        return createNewWatchlist(telegramUserId, animeId);
    }

    public Watchlist setStatus(Long telegramUserId, Long animeId, WatchStatus newStatus) {
        Watchlist watchlist = repository.findByTelegramUserIdAndAnimeId(telegramUserId, animeId)
                .orElseGet(() -> createNewWatchlist(telegramUserId, animeId));

        Integer newEpisode = watchlist.lastWatchedEpisode();

        // Marking as COMPLETED implies all available episodes were watched
        if (newStatus == WatchStatus.COMPLETED) {
            Optional<Anime> animeOpt = animeService.findById(animeId);
            if (animeOpt.isPresent()) {
                Anime anime = animeOpt.get();
                if (anime.episodes() != null && anime.episodes() > 0) {
                    newEpisode = anime.episodes();
                }
            }
        }

        Watchlist updated = new Watchlist(
                watchlist.id(),
                watchlist.telegramUserId(),
                watchlist.animeId(),
                newStatus,
                newEpisode,
                watchlist.addedAt(),
                LocalDateTime.now()
        );
        return repository.save(updated);
    }

    private Watchlist createNewWatchlist(Long telegramUserId, Long animeId) {
        return repository.save(new Watchlist(
                null,
                telegramUserId,
                animeId,
                WatchStatus.WATCHING,
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
        ));
    }

    // New helper to expose all watchlist entries for a user
    public List<Watchlist> findByTelegramUserId(Long telegramUserId) {
        return repository.findByTelegramUserId(telegramUserId);
    }
}
