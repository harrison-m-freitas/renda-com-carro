package dev.harrison.rendacomcarro.finance;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.application.FormDraftService.SaveDraftCommand;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.finance.application.AcquisitionPlanService;
import dev.harrison.rendacomcarro.finance.application.FinancialObligationService;
import dev.harrison.rendacomcarro.finance.domain.InterestRatePeriod;
import dev.harrison.rendacomcarro.finance.domain.ObligationCalculationMethod;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.domain.ObligationType;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=obligation-web-owner",
    "APP_ADMIN_PASSWORD=obligation-web-owner-password"
})
@Transactional
class ObligationWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired FormDraftService drafts;
    @Autowired ObjectMapper mapper;
    @Autowired FinancialObligationService obligations;
    @Autowired AcquisitionPlanService acquisitionPlans;

    @Test
    @WithMockUser(username = "obligation-web-owner", roles = "OWNER")
    void obligationFormUsesRedesignedInputsAndSingleDraftMetadata() throws Exception {
        mvc.perform(get("/obligations/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-guided-form")))
            .andExpect(content().string(containsString("data-draft-type=\"OBLIGATION\"")))
            .andExpect(content().string(containsString("data-draft-schema-version=\"2\"")))
            .andExpect(content().string(containsString("data-draft-context-key=\"draft:")))
            .andExpect(content().string(containsString("data-draft-recovery-mode=\"none\"")))
            .andExpect(content().string(containsString("data-form-step=\"1\"")))
            .andExpect(content().string(containsString("data-form-step=\"4\"")))
            .andExpect(content().string(containsString("Valor emprestado ou financiado")))
            .andExpect(content().string(containsString("Conheço o valor da parcela")))
            .andExpect(content().string(containsString("data-financial-money")))
            .andExpect(content().string(containsString("data-financial-percent")))
            .andExpect(content().string(containsString("Sair e manter rascunho")))
            .andExpect(content().string(containsString("Descartar rascunho")))
            .andExpect(content().string(containsString("type=\"module\"")));
    }

    @Test
    @WithMockUser(username = "obligation-web-owner", roles = "OWNER")
    void postsInterestFreeFlexibleObligationUsingBrazilianValues() throws Exception {
        mvc.perform(post("/obligations")
                .with(csrf())
                .param("draftKey", "draft:" + UUID.randomUUID())
                .param("type", "FAMILY_LOAN")
                .param("mode", "FLEXIBLE_PAYMENTS")
                .param("calculationMethod", "INTEREST_FREE")
                .param("creditor", "Família")
                .param("principalAmount", "30.000,00")
                .param("startDate", "2026-07-13")
                .param("monthlyTarget", "500,00"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/obligations/*"));
    }

    @Test
    @WithMockUser(username = "obligation-web-owner", roles = "OWNER")
    void obligationListAndNewEntryExposeTheSingleOwnerDraft() throws Exception {
        String ownKey = "draft:" + UUID.randomUUID();
        drafts.save("obligation-web-owner", new SaveDraftCommand(
            FormDraftType.OBLIGATION,
            ownKey,
            2,
            1,
            null,
            mapper.createObjectNode()
                .put("creditor", "Banco em rascunho")
                .put("type", "BANK_FINANCING"),
            false
        ));

        mvc.perform(get("/obligations"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Obrigação em andamento")))
            .andExpect(content().string(containsString("Banco em rascunho")))
            .andExpect(content().string(containsString(ownKey)))
            .andExpect(content().string(containsString("Descartar")));

        mvc.perform(get("/obligations/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Você já possui uma obrigação em andamento")))
            .andExpect(content().string(containsString("Continuar rascunho")))
            .andExpect(content().string(containsString("Descartar e começar novamente")))
            .andExpect(content().string(containsString(ownKey)));

        mvc.perform(get("/obligations/new").param("draftKey", ownKey))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-draft-recovery-mode=\"auto\"")))
            .andExpect(content().string(containsString("data-draft-context-key=\"" + ownKey + "\"")));
    }

    @Test
    @WithMockUser(username = "obligation-web-owner", roles = "OWNER")
    void purchasePlanContextSurvivesNewDraftDecisionAndSuccessfulSubmission() throws Exception {
        var plan = acquisitionPlans.create(new AcquisitionPlanService.CreateCommand(
            null,
            "Compra do veículo",
            new BigDecimal("10000.00"),
            BigDecimal.ZERO,
            LocalDate.of(2026, 7, 14),
            null
        ));

        mvc.perform(get("/obligations/new").param("acquisitionPlanId", plan.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"acquisitionPlanId\"")))
            .andExpect(content().string(containsString("value=\"" + plan.getId() + "\"")))
            .andExpect(content().string(containsString("Esta obrigação será somada às outras fontes do plano")));

        String draftKey = "draft:" + UUID.randomUUID();
        drafts.save("obligation-web-owner", new SaveDraftCommand(
            FormDraftType.OBLIGATION,
            draftKey,
            2,
            1,
            null,
            mapper.createObjectNode()
                .put("creditor", "Familiar")
                .put("type", "FAMILY_LOAN")
                .put("acquisitionPlanId", plan.getId().toString()),
            false
        ));

        mvc.perform(get("/obligations/new").param("acquisitionPlanId", plan.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Você já possui uma obrigação em andamento")))
            .andExpect(content().string(containsString("name=\"acquisitionPlanId\"")))
            .andExpect(content().string(containsString("value=\"" + plan.getId() + "\"")));

        mvc.perform(post("/obligations/draft/discard")
                .with(csrf())
                .param("next", "new")
                .param("acquisitionPlanId", plan.getId().toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(
                "/obligations/new?fresh=true&acquisitionPlanId=" + plan.getId()
            ));

        mvc.perform(post("/obligations")
                .with(csrf())
                .param("draftKey", "draft:" + UUID.randomUUID())
                .param("acquisitionPlanId", plan.getId().toString())
                .param("type", "FAMILY_LOAN")
                .param("mode", "FLEXIBLE_PAYMENTS")
                .param("calculationMethod", "INTEREST_FREE")
                .param("creditor", "Familiar")
                .param("principalAmount", "10.000,00")
                .param("startDate", "2026-07-14")
                .param("monthlyTarget", "500,00"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/acquisition-plans/" + plan.getId()));
    }

    @Test
    @WithMockUser(username = "obligation-web-owner", roles = "OWNER")
    void scheduleDoesNotClaimInstallmentPaymentStatusesWithoutReconciliation() throws Exception {
        var obligation = obligations.create(new FinancialObligationService.CreateCommand(
            null,
            null,
            ObligationType.BANK_FINANCING,
            ObligationMode.FIXED_INSTALLMENTS,
            ObligationCalculationMethod.INTEREST_FREE,
            "Banco",
            new BigDecimal("1200.00"),
            BigDecimal.ZERO,
            InterestRatePeriod.MONTHLY,
            LocalDate.of(2026, 7, 14),
            LocalDate.of(2026, 8, 14),
            12,
            null,
            null,
            null,
            null
        ));

        mvc.perform(get("/obligations/{id}", obligation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Cronograma planejado")))
            .andExpect(content().string(not(containsString("<th>Status</th>"))))
            .andExpect(content().string(not(containsString(">Pendente<"))));
    }

    @Test
    @WithMockUser(username = "obligation-web-owner", roles = "OWNER")
    void duplicatePaymentReferenceReturnsAFieldErrorInsteadOfServerError() throws Exception {
        var obligation = createFlexibleObligation();
        obligations.pay(
            obligation.getId(),
            LocalDate.of(2026, 7, 15),
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "PAY-1",
            null
        );

        mvc.perform(post("/obligations/{id}/payments", obligation.getId())
                .with(csrf())
                .param("date", "2026-07-16")
                .param("principal", "100.00")
                .param("interest", "0.00")
                .param("extra", "0.00")
                .param("externalReference", " PAY-1 "))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Já existe um pagamento com essa referência")))
            .andExpect(content().string(containsString("is-invalid")));
    }

    private dev.harrison.rendacomcarro.finance.domain.FinancialObligation createFlexibleObligation() {
        return obligations.create(new FinancialObligationService.CreateCommand(
            null,
            null,
            ObligationType.FAMILY_LOAN,
            ObligationMode.FLEXIBLE_PAYMENTS,
            ObligationCalculationMethod.INTEREST_FREE,
            "Família",
            new BigDecimal("30000.00"),
            BigDecimal.ZERO,
            InterestRatePeriod.MONTHLY,
            LocalDate.of(2026, 7, 14),
            null,
            null,
            null,
            null,
            new BigDecimal("500.00"),
            null
        ));
    }
}
