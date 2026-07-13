package dev.harrison.rendacomcarro.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.harrison.rendacomcarro.draft.domain.FormDraft;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.draft.infrastructure.FormDraftRepository;
import dev.harrison.rendacomcarro.security.domain.AppUser;
import dev.harrison.rendacomcarro.security.infrastructure.AppUserRepository;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=draft-owner",
    "APP_ADMIN_PASSWORD=draft-owner-password"
})
class FormDraftPersistenceTest extends PostgresIntegrationTest {
    @Autowired AppUserRepository users;
    @Autowired FormDraftRepository drafts;

    @Test
    void storesJsonAndEnforcesOneDraftPerOwnerTypeAndContext() {
        AppUser owner = users.findByUsername("draft-owner").orElseThrow();
        LocalDateTime now = LocalDateTime.of(2026, 7, 13, 10, 0);

        drafts.saveAndFlush(FormDraft.create(
            owner,
            FormDraftType.EXPENSE,
            "current",
            1,
            1,
            "{\"amount\":\"120,50\"}",
            now,
            now.plusDays(7)
        ));

        FormDraft restored = drafts.findByOwnerUsernameAndFormTypeAndContextKey(
            "draft-owner", FormDraftType.EXPENSE, "current"
        ).orElseThrow();

        assertThat(restored.getPayloadJson()).isEqualTo("{\"amount\":\"120,50\"}");
        assertThat(restored.getVersion()).isZero();

        assertThatThrownBy(() -> drafts.saveAndFlush(FormDraft.create(
            owner,
            FormDraftType.EXPENSE,
            "current",
            1,
            1,
            "{}",
            now,
            now.plusDays(7)
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }
}
