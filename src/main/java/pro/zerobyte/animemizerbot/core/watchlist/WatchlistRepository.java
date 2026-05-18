package pro.zerobyte.animemizerbot.core.watchlist;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends ListCrudRepository<Watchlist, Long> {

    List<Watchlist> findByTelegramUserId(Long telegramUserId);

    List<Watchlist> findByAnimeId(Long animeId);

    Optional<Watchlist> findByTelegramUserIdAndAnimeId(Long telegramUserId, Long animeId);
}
