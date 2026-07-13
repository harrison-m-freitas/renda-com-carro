package dev.harrison.rendacomcarro.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.application.FormDraftService.SaveDraftCommand;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.finance.application.ObligationFormSubmissionService;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.domain.ObligationType;
import dev.harrison.rendacomcarro.finance.web.ObligationForm;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=obligation-submission-owner",
    "APP_ADMIN_PASSWORD=obligation-submission-password"
})
@Transactional
class ObligationFormSubmissionServiceTest extends PostgresIntegrationTest {
    @Autowired ObligationFormSubmissionService submissions;
    @Autowired FormDraftService drafts;
    @Autowired ObjectMapper mapper;

    @Test
    void createsObligationConvertsAnnualPercentAndDeletesExactDraft() {
        ObligationForm form = flexibleForm();
        seedDraft(form);

        var obligation = submissions.submit("obligation-submission-owner", form);

        assertThat(obligation.getAnnualInterestRate()).isEqualByComparingTo("0.120000");
        assertThat(drafts.find(
            "obligation-submission-owner", FormDraftType.OBLIGATION, form.draftContextKey()
        )).isEmpty();
    }

    @Test
    void failedStructuredSubmissionPreservesDraft() {
        ObligationForm form = flexibleForm();
        form.setMode(ObligationMode.STRUCTURED);
        form.setFirstDueDate(null);
        form.setTermMonths(null);
        seedDraft(form);

        assertThatThrownBy(() -> submissions.submit("obligation-submission-owner", form))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cronograma");
        assertThat(drafts.find(
            "obligation-submission-owner", FormDraftType.OBLIGATION, form.draftContextKey()
        )).isPresent();
    }

    private ObligationForm flexibleForm() {
        ObligationForm form = new ObligationForm();
        form.setDraftKey("draft:" + UUID.randomUUID());
        form.setType(ObligationType.FAMILY_LOAN);
        form.setMode(ObligationMode.FLEXIBLE);
        form.setCreditor("Família");
        form.setPrincipal(new BigDecimal("30000.00"));
        form.setAnnualRatePercent(new BigDecimal("12"));
        form.setStartDate(LocalDate.of(2026, 7, 13));
        form.setMonthlyTarget(new BigDecimal("500.00"));
        return form;
    }

    private void seedDraft(ObligationForm form) {
        var payload = mapper.createObjectNode()
            .put("creditor", form.getCreditor())
            .put("type", form.getType().name())
            .put("mode", form.getMode().name())
            .put("principal", form.getPrincipal().toPlainString())
            .put("annualRatePercent", form.getAnnualRatePercent().toPlainString())
            .put("startDate", form.getStartDate().toString());
        if (form.getMode() == ObligationMode.FLEXIBLE) {
            payload.put("monthlyTarget", form.getMonthlyTarget().toPlainString());
        } else {
            payload.put("firstDueDate", "")
                .put("termMonths", "");
        }
        drafts.save("obligation-submission-owner", new SaveDraftCommand(
            FormDraftType.OBLIGATION,
            form.draftContextKey(),
            1,
            2,
            null,
            payload,
            false
        ));
    }
}
