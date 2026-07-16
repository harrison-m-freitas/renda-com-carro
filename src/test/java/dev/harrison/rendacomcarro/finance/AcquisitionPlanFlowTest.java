package dev.harrison.rendacomcarro.finance;

import static org.assertj.core.api.Assertions.assertThat;

import dev.harrison.rendacomcarro.finance.application.AcquisitionPlanService;
import dev.harrison.rendacomcarro.finance.application.FinancialObligationService;
import dev.harrison.rendacomcarro.finance.domain.InterestRatePeriod;
import dev.harrison.rendacomcarro.finance.domain.ObligationCalculationMethod;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.domain.ObligationType;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=acquisition-owner",
    "APP_ADMIN_PASSWORD=acquisition-owner-password"
})
@Transactional
class AcquisitionPlanFlowTest extends PostgresIntegrationTest {
    @Autowired AcquisitionPlanService plans;
    @Autowired FinancialObligationService obligations;

    @Test
    void purchaseCanBeCoveredByFamilyLoanAndBankFinancing() {
        var plan = plans.create(new AcquisitionPlanService.CreateCommand(
            null,
            "Compra do Renault",
            new BigDecimal("45000.00"),
            BigDecimal.ZERO,
            LocalDate.of(2026, 7, 14),
            null
        ));

        obligations.create(new FinancialObligationService.CreateCommand(
            null,
            plan.getId(),
            ObligationType.FAMILY_LOAN,
            ObligationMode.FLEXIBLE_PAYMENTS,
            ObligationCalculationMethod.INTEREST_FREE,
            "Mãe",
            new BigDecimal("10000.00"),
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
        obligations.create(new FinancialObligationService.CreateCommand(
            null,
            plan.getId(),
            ObligationType.BANK_FINANCING,
            ObligationMode.FIXED_INSTALLMENTS,
            ObligationCalculationMethod.INSTALLMENT_KNOWN,
            "Banco",
            new BigDecimal("35000.00"),
            null,
            null,
            LocalDate.of(2026, 7, 14),
            LocalDate.of(2026, 8, 10),
            36,
            new BigDecimal("1386.00"),
            null,
            null,
            null
        ));

        var summary = plans.summary(plan.getId());

        assertThat(summary.obligationPrincipalAmount()).isEqualByComparingTo("45000.00");
        assertThat(summary.totalFundingAmount()).isEqualByComparingTo("45000.00");
        assertThat(summary.remainingAmount()).isEqualByComparingTo("0.00");
        assertThat(summary.obligations()).hasSize(2);
        assertThat(summary.totalsComplete()).isTrue();
        assertThat(summary.plannedRepaymentAmount()).isGreaterThan(new BigDecimal("45000.00"));
        assertThat(summary.financingCostAmount()).isGreaterThan(BigDecimal.ZERO);
    }
    @Test
    void unknownRateKeepsPlanRepaymentAndCostExplicitlyPartial() {
        var plan = plans.create(new AcquisitionPlanService.CreateCommand(
            null,
            "Compra com taxa pendente",
            new BigDecimal("20000.00"),
            BigDecimal.ZERO,
            LocalDate.of(2026, 7, 14),
            null
        ));

        var knownObligation = obligations.create(new FinancialObligationService.CreateCommand(
            null,
            plan.getId(),
            ObligationType.BANK_FINANCING,
            ObligationMode.FIXED_INSTALLMENTS,
            ObligationCalculationMethod.INSTALLMENT_KNOWN,
            "Banco",
            new BigDecimal("10000.00"),
            null,
            null,
            LocalDate.of(2026, 7, 14),
            LocalDate.of(2026, 8, 14),
            12,
            new BigDecimal("950.00"),
            null,
            null,
            null
        ));
        obligations.create(new FinancialObligationService.CreateCommand(
            null,
            plan.getId(),
            ObligationType.FAMILY_LOAN,
            ObligationMode.FLEXIBLE_PAYMENTS,
            ObligationCalculationMethod.RATE_UNKNOWN,
            "Familiar",
            new BigDecimal("10000.00"),
            null,
            null,
            LocalDate.of(2026, 7, 14),
            null,
            null,
            null,
            null,
            new BigDecimal("500.00"),
            null
        ));

        var summary = plans.summary(plan.getId());

        assertThat(summary.totalsComplete()).isFalse();
        assertThat(summary.plannedRepaymentAmount())
            .isEqualByComparingTo(knownObligation.getPlannedTotalAmount());
        assertThat(summary.financingCostAmount()).isEqualByComparingTo(
            knownObligation.getPlannedTotalAmount().subtract(knownObligation.getPrincipalAmount())
        );
    }

}
