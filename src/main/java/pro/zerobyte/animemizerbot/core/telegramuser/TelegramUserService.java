package pro.zerobyte.animemizerbot.core.telegramuser;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TelegramUserService {

    private final TelegramUserRepository telegramUserRepository;

    public TelegramUserService(TelegramUserRepository telegramUserRepository) {
        this.telegramUserRepository = telegramUserRepository;
    }

    public Optional<TelegramUser> findByTelegramId(Long telegramId) {
        return telegramUserRepository.findByTelegramId(telegramId);
    }

    public TelegramUser createOrUpdate(User user, Long chatId) {
        TelegramUser telegramUser = telegramUserRepository.findByTelegramId(user.getId())
                .map(existingUser -> new TelegramUser(
                        existingUser.id(),
                        user.getId(),
                        chatId,
                        user.getUserName(),
                        user.getFirstName(),
                        user.getLastName(),
                        existingUser.registeredAt(),
                        true))
                .orElseGet(() -> new TelegramUser(
                        null,
                        user.getId(),
                        chatId,
                        user.getUserName(),
                        user.getFirstName(),
                        user.getLastName(),
                        LocalDateTime.now(),
                        true));
        return telegramUserRepository.save(telegramUser);
    }
}
