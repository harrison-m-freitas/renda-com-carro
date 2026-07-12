package dev.harrison.rendacomcarro;

import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=smoke-owner",
    "APP_ADMIN_PASSWORD=smoke-owner-credential"
})
class ApplicationSmokeTest extends PostgresIntegrationTest {
    @Test
    void contextLoads() {
    }
}
