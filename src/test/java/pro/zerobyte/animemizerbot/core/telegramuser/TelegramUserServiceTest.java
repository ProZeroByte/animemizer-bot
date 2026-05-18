package pro.zerobyte.animemizerbot.core.telegramuser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramUserServiceTest {

    @Mock
    private TelegramUserRepository telegramUserRepository;

    @InjectMocks
    private TelegramUserService telegramUserService;

    private User newTelegramUser(Long id, String userName, String firstName, String lastName) {
        User user = new User(id, firstName, false);
        user.setUserName(userName);
        user.setLastName(lastName);
        return user;
    }

    @Nested
    @DisplayName("findByTelegramId")
    class FindByTelegramId {

        @Test
        void should_returnUser_when_exists() {
            // Given
            Long telegramId = 42L;
            TelegramUser existingUser = new TelegramUser(1L, telegramId, 100L, "user1",
                    "John", "Doe", LocalDateTime.of(2024, 1, 1, 0, 0), true);
            when(telegramUserRepository.findByTelegramId(telegramId)).thenReturn(Optional.of(existingUser));

            // When
            Optional<TelegramUser> result = telegramUserService.findByTelegramId(telegramId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().telegramId()).isEqualTo(telegramId);
            assertThat(result.get().userName()).isEqualTo("user1");
        }

        @Test
        void should_returnEmpty_when_notExists() {
            // Given
            Long telegramId = 42L;
            when(telegramUserRepository.findByTelegramId(telegramId)).thenReturn(Optional.empty());

            // When
            Optional<TelegramUser> result = telegramUserService.findByTelegramId(telegramId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createOrUpdate")
    class CreateOrUpdate {

        @Test
        void should_createNewUser_when_notExists() {
            // Given
            User user = newTelegramUser(42L, "user1", "John", "Doe");
            Long chatId = 100L;
            when(telegramUserRepository.findByTelegramId(42L)).thenReturn(Optional.empty());

            TelegramUser savedUser = new TelegramUser(1L, 42L, chatId, "user1",
                    "John", "Doe", LocalDateTime.of(2024, 6, 1, 12, 0), true);
            when(telegramUserRepository.save(any(TelegramUser.class))).thenReturn(savedUser);

            // When
            TelegramUser result = telegramUserService.createOrUpdate(user, chatId);

            // Then
            assertThat(result.telegramId()).isEqualTo(42L);
            assertThat(result.chatId()).isEqualTo(chatId);
            assertThat(result.userName()).isEqualTo("user1");
            assertThat(result.firstName()).isEqualTo("John");
            assertThat(result.lastName()).isEqualTo("Doe");
            assertThat(result.isActive()).isTrue();
        }

        @Test
        void should_updateExistingUser_when_exists() {
            // Given
            User user = newTelegramUser(42L, "updatedUser", "Jane", "Smith");
            Long chatId = 200L;
            TelegramUser existingUser = new TelegramUser(1L, 42L, 100L, "user1",
                    "John", "Doe", LocalDateTime.of(2024, 1, 1, 0, 0), true);
            when(telegramUserRepository.findByTelegramId(42L)).thenReturn(Optional.of(existingUser));

            TelegramUser updatedUser = new TelegramUser(1L, 42L, chatId, "updatedUser",
                    "Jane", "Smith", existingUser.registeredAt(), true);
            when(telegramUserRepository.save(any(TelegramUser.class))).thenReturn(updatedUser);

            // When
            TelegramUser result = telegramUserService.createOrUpdate(user, chatId);

            // Then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.telegramId()).isEqualTo(42L);
            assertThat(result.chatId()).isEqualTo(chatId);
            assertThat(result.userName()).isEqualTo("updatedUser");
            assertThat(result.firstName()).isEqualTo("Jane");
            assertThat(result.lastName()).isEqualTo("Smith");
        }

        @Test
        void should_preserveRegisteredAt_when_updating() {
            // Given
            LocalDateTime originalRegisteredAt = LocalDateTime.of(2024, 1, 1, 0, 0);
            User user = newTelegramUser(42L, "updatedUser", "Jane", "Smith");
            Long chatId = 200L;
            TelegramUser existingUser = new TelegramUser(1L, 42L, 100L, "user1",
                    "John", "Doe", originalRegisteredAt, true);
            when(telegramUserRepository.findByTelegramId(42L)).thenReturn(Optional.of(existingUser));

            TelegramUser updatedUser = new TelegramUser(1L, 42L, chatId, "updatedUser",
                    "Jane", "Smith", originalRegisteredAt, true);
            when(telegramUserRepository.save(any(TelegramUser.class))).thenReturn(updatedUser);

            // When
            TelegramUser result = telegramUserService.createOrUpdate(user, chatId);

            // Then
            assertThat(result.registeredAt()).isEqualTo(originalRegisteredAt);
        }
    }
}
