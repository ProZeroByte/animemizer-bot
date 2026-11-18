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
            WatchStatus currentStatus = watchlist.status();
            WatchStatus newStatus = currentStatus;
            if (currentStatus != null && newEpisode >= max && max != Integer.MAX_VALUE) {
                newStatus = currentStatus.onAllEpisodesWatched();
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

    /**
     * Applies the requested status, or clears it when the same status is selected again.
     * Clearing keeps the watched-episode progress and leaves the anime without a status.
     */
    public Watchlist toggleStatus(Long telegramUserId, Long animeId, WatchStatus status) {
        Optional<Watchlist> existing = repository.findByTelegramUserIdAndAnimeId(telegramUserId, animeId);
        if (existing.isPresent() && existing.get().status() == status) {
            return clearStatus(existing.get());
        }
        return setStatus(telegramUserId, animeId, status);
    }

    private Watchlist clearStatus(Watchlist watchlist) {
        Watchlist cleared = new Watchlist(
                watchlist.id(),
                watchlist.telegramUserId(),
                watchlist.animeId(),
                null,
                watchlist.lastWatchedEpisode(),
                watchlist.addedAt(),
                LocalDateTime.now()
        );
        return repository.save(cleared);
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
