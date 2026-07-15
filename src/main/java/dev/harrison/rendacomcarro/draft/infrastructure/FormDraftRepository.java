package dev.harrison.rendacomcarro.draft.infrastructure;

import dev.harrison.rendacomcarro.draft.domain.FormDraft;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import java.time.LocalDateTime;
import java.util.Collection;
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

    List<FormDraft> findAllByOwnerUsernameAndFormTypeAndExpiresAtAfterOrderByUpdatedAtDescIdDesc(
        String username,
        FormDraftType formType,
        LocalDateTime now
    );

    Optional<FormDraft> findFirstByOwnerUsernameAndFormTypeAndExpiresAtAfterOrderByUpdatedAtDescIdDesc(
        String username,
        FormDraftType formType,
        LocalDateTime now
    );

    long deleteByOwnerUsernameAndFormTypeAndContextKey(
        String username,
        FormDraftType formType,
        String contextKey
    );

    long deleteByOwnerUsernameAndFormTypeAndContextKeyIn(
        String username,
        FormDraftType formType,
        Collection<String> contextKeys
    );

    long deleteByOwnerUsernameAndFormType(
        String username,
        FormDraftType formType
    );

    long deleteByOwnerUsernameAndFormTypeAndExpiresAtLessThanEqual(
        String username,
        FormDraftType formType,
        LocalDateTime cutoff
    );

    long deleteByExpiresAtLessThanEqual(LocalDateTime cutoff);
}
