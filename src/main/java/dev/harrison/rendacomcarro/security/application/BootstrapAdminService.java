package dev.harrison.rendacomcarro.security.application;

import dev.harrison.rendacomcarro.security.domain.AppUser;
import dev.harrison.rendacomcarro.security.infrastructure.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminService implements ApplicationRunner {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;

    public BootstrapAdminService(
        AppUserRepository users,
        PasswordEncoder encoder,
        @Value("${APP_ADMIN_USERNAME:}") String username,
        @Value("${APP_ADMIN_PASSWORD:}") String password
    ) {
        this.users = users;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        if (username.isBlank() || password.length() < 16) {
            throw new IllegalStateException(
                "Configure APP_ADMIN_USERNAME e APP_ADMIN_PASSWORD com ao menos 16 caracteres"
            );
        }
        users.save(new AppUser(username, encoder.encode(password)));
    }
}
