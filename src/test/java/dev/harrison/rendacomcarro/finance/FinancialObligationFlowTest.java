package dev.harrison.rendacomcarro.finance;

import static org.assertj.core.api.Assertions.assertThat;

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
    "APP_ADMIN_USERNAME=finance-owner",
    "APP_ADMIN_PASSWORD=finance-owner-credential"
})
@Transactional
class FinancialObligationFlowTest extends PostgresIntegrationTest {
    @Autowired FinancialObligationService service;

    @Test
    void knownInstallmentCreatesTheContractualScheduleAndPersistsInferredRates() {
        var obligation = service.create(new FinancialObligationService.CreateCommand(
            null,
            null,
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

        var persisted = service.get(obligation.getId());
        var schedule = service.schedule(obligation.getId());

        assertThat(persisted.getInstallmentAmount()).isEqualByComparingTo("1386.00");
        assertThat(persisted.getMonthlyInterestRate()).isGreaterThan(BigDecimal.ZERO);
        assertThat(persisted.getAnnualEffectiveInterestRate()).isGreaterThan(BigDecimal.ZERO);
        assertThat(persisted.getPlannedTotalAmount()).isGreaterThan(new BigDecimal("49000.00"));
        assertThat(persisted.getPlannedInterestAmount()).isEqualByComparingTo(
            persisted.getPlannedTotalAmount().subtract(new BigDecimal("35000.00"))
        );
        assertThat(schedule).hasSize(36);
        assertThat(schedule.get(0).getExpectedAmount()).isEqualByComparingTo("1386.00");
    }

    @Test
    void flexiblePaymentReducesPrincipalWithoutBecomingOperatingExpense() {
        var obligation = service.create(new FinancialObligationService.CreateCommand(
            null,
            null,
            ObligationType.FAMILY_LOAN,
            ObligationMode.FLEXIBLE_PAYMENTS,
            ObligationCalculationMethod.INTEREST_FREE,
            "Família",
            new BigDecimal("30000.00"),
            BigDecimal.ZERO,
            InterestRatePeriod.MONTHLY,
            LocalDate.now(),
            null,
            null,
            null,
            null,
            new BigDecimal("500.00"),
            null
        ));

        service.pay(
            obligation.getId(),
            LocalDate.now(),
            new BigDecimal("500.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "PAY-1",
            null
        );

        assertThat(service.get(obligation.getId()).getCurrentBalance())
            .isEqualByComparingTo("29500.00");
    }
    @Test
    void multiplePaymentsWithoutAReferenceArePersistedWithNullReferences() {
        var obligation = createFlexibleObligation();

        service.pay(
            obligation.getId(),
            LocalDate.of(2026, 7, 15),
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "   ",
            null
        );
        service.pay(
            obligation.getId(),
            LocalDate.of(2026, 7, 16),
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "",
            null
        );

        assertThat(service.paymentHistory(obligation.getId()))
            .hasSize(2)
            .allSatisfy(payment -> assertThat(payment.getExternalReference()).isNull());
    }

    private dev.harrison.rendacomcarro.finance.domain.FinancialObligation createFlexibleObligation() {
        return service.create(new FinancialObligationService.CreateCommand(
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
