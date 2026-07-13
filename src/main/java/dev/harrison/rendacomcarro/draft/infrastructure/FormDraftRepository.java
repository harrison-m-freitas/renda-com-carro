package dev.harrison.rendacomcarro.draft.infrastructure;

import dev.harrison.rendacomcarro.draft.domain.FormDraft;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormDraftRepository extends JpaRepository<FormDraft, UUID> {
    Optional<FormDraft> findByOwnerUsernameAndFormTypeAndContextKey(
        String username,
        FormDraftType formType,
        String contextKey
    );

    List<FormDraft> findAllByOwnerUsernameAndFormTypeAndExpiresAtAfterOrderByUpdatedAtDesc(
        String username,
        FormDraftType formType,
        LocalDateTime now
    );

    long deleteByExpiresAtLessThanEqual(LocalDateTime cutoff);
}
