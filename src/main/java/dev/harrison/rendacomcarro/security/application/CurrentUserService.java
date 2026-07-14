package dev.harrison.rendacomcarro.security.application;

import dev.harrison.rendacomcarro.security.domain.AppUser;
import dev.harrison.rendacomcarro.security.infrastructure.AppUserRepository;
import dev.harrison.rendacomcarro.shared.domain.ResourceNotFoundException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {
    private final AppUserRepository users;

    public CurrentUserService(AppUserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> find(String username) {
        return users.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public AppUser require(String username) {
        return find(username)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuário autenticado não encontrado."
            ));
    }
}
