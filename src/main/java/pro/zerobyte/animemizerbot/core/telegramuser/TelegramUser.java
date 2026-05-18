package pro.zerobyte.animemizerbot.core.telegramuser;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("telegram_user")
public record TelegramUser(
        @Id Long id,
        Long telegramId,
        Long chatId,
        String userName,
        String firstName,
        String lastName,
        LocalDateTime registeredAt,
        Boolean isActive
) {}
