package pro.zerobyte.animemizerbot.core.telegramuser;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelegramUserRepository extends ListCrudRepository<TelegramUser, Long> {

    Optional<TelegramUser> findByTelegramId(Long telegramId);
}
