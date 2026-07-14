package dev.harrison.rendacomcarro.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.application.FormDraftConflictException;
import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.application.FormDraftService.SaveDraftCommand;
import dev.harrison.rendacomcarro.draft.domain.FormDraft;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.draft.infrastructure.FormDraftRepository;
import dev.harrison.rendacomcarro.security.domain.AppUser;
import dev.harrison.rendacomcarro.security.infrastructure.AppUserRepository;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(FormDraftServiceTest.ClockConfiguration.class)
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=draft-service-owner",
    "APP_ADMIN_PASSWORD=draft-service-owner-password"
})
@Transactional
class FormDraftServiceTest extends PostgresIntegrationTest {
    @Autowired FormDraftService service;
    @Autowired FormDraftRepository repository;
    @Autowired AppUserRepository users;
    @Autowired ObjectMapper mapper;
    @Autowired Clock clock;

    @BeforeEach
    void ensureSecondOwner() {
        if (users.findByUsername("other-draft-owner").isEmpty()) {
            users.save(new AppUser("other-draft-owner", "unused-test-password-hash"));
        }
    }

    @Test
    void createsUpdatesConflictsAndForceReplaces() {
        var first = service.save("draft-service-owner", expenseCommand(null, false, "100,00"));
        assertThat(first.version()).isZero();
        assertThat(first.expiresAt()).isEqualTo(LocalDateTime.now(clock).plusDays(7));

        var second = service.save("draft-service-owner", expenseCommand(0L, false, "120,00"));
        assertThat(second.version()).isEqualTo(1);
        assertThat(second.payload().path("amount").asText()).isEqualTo("120.00");

        assertThatThrownBy(() -> service.save(
            "draft-service-owner", expenseCommand(0L, false, "130,00")
        )).isInstanceOfSatisfying(FormDraftConflictException.class, exception ->
            assertThat(exception.getCurrent().version()).isEqualTo(1)
        );

        var forced = service.save("draft-service-owner", expenseCommand(0L, true, "140,00"));
        assertThat(forced.version()).isEqualTo(2);
        assertThat(forced.payload().path("amount").asText()).isEqualTo("140.00");
    }

    @Test
    void expiredDraftIsNotReturnedAndCleanupDeletesIt() {
        AppUser owner = users.findByUsername("draft-service-owner").orElseThrow();
        LocalDateTime now = LocalDateTime.now(clock);
        repository.saveAndFlush(FormDraft.create(
            owner,
            FormDraftType.EXPENSE,
            "current",
            2,
            1,
            "{}",
            now.minusDays(8),
            now.minusSeconds(1)
        ));

        assertThat(service.find("draft-service-owner", FormDraftType.EXPENSE, "current"))
            .isEmpty();
        assertThat(service.deleteExpired()).isEqualTo(1);
    }

    @Test
    void discardChecksVersionAndCompleteIsIdempotent() {
        service.save("draft-service-owner", expenseCommand(null, false, "100,00"));
        service.save("draft-service-owner", expenseCommand(0L, false, "110,00"));

        assertThatThrownBy(() -> service.discard(
            "draft-service-owner", FormDraftType.EXPENSE, "current", 0L
        )).isInstanceOf(FormDraftConflictException.class);

        service.discard("draft-service-owner", FormDraftType.EXPENSE, "current", 1L);
        assertThat(service.find("draft-service-owner", FormDraftType.EXPENSE, "current"))
            .isEmpty();
        assertThatNoException().isThrownBy(() -> service.complete(
            "draft-service-owner", FormDraftType.EXPENSE, "current"
        ));
    }

    @Test
    void oversizedPayloadIsRejected() {
        ObjectNode payload = validExpensePayload("100,00");
        payload.put("notes", "x".repeat(66_000));

        assertThatThrownBy(() -> service.save(
            "draft-service-owner",
            new SaveDraftCommand(
                FormDraftType.EXPENSE,
                "current",
                2,
                1,
                null,
                payload,
                false
            )
        )).isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("64 KiB");
    }

    @Test
    void obligationListContainsOnlyActiveDraftsForOwner() {
        service.save("draft-service-owner", obligationCommand("draft:" + UUID.randomUUID()));
        service.save("other-draft-owner", obligationCommand("draft:" + UUID.randomUUID()));

        assertThat(service.listActive("draft-service-owner", FormDraftType.OBLIGATION))
            .hasSize(1)
            .allSatisfy(view -> assertThat(view.formType()).isEqualTo(FormDraftType.OBLIGATION));
    }

    private SaveDraftCommand expenseCommand(Long version, boolean force, String amount) {
        return new SaveDraftCommand(
            FormDraftType.EXPENSE,
            "current",
            2,
            1,
            version,
            validExpensePayload(amount),
            force
        );
    }

    private ObjectNode validExpensePayload(String amount) {
        return mapper.createObjectNode()
            .put("vehicleId", UUID.randomUUID().toString())
            .put("categoryId", UUID.randomUUID().toString())
            .put("expenseDate", "2026-07-13")
            .put("paymentStatus", "PENDING")
            .put("amount", amount);
    }

    private SaveDraftCommand obligationCommand(String key) {
        ObjectNode payload = mapper.createObjectNode()
            .put("creditor", "Credor")
            .put("type", "FAMILY_LOAN");
        return new SaveDraftCommand(
            FormDraftType.OBLIGATION,
            key,
            1,
            1,
            null,
            payload,
            false
        );
    }

    @TestConfiguration
    static class ClockConfiguration {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(
                Instant.parse("2026-07-13T13:00:00Z"),
                ZoneId.of("America/Sao_Paulo")
            );
        }
    }
}
