package pro.zerobyte.animemizerbot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pro.zerobyte.animemizerbot.config.TestTelegramConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestTelegramConfig.class)
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
