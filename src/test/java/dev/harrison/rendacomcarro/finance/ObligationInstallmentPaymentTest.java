package dev.harrison.rendacomcarro.finance;

import static org.assertj.core.api.Assertions.assertThat;

import dev.harrison.rendacomcarro.finance.application.FinancialObligationService;
import dev.harrison.rendacomcarro.finance.domain.InstallmentStatus;
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
    "APP_ADMIN_USERNAME=installment-owner",
    "APP_ADMIN_PASSWORD=installment-owner-password"
})
@Transactional
class ObligationInstallmentPaymentTest extends PostgresIntegrationTest {
    @Autowired FinancialObligationService service;

    @Test
    void partialPaymentLeavesOnlyTheRemainingInstallmentAmount() {
        var obligation = structuredObligation();

        service.pay(
            obligation.getId(),
            LocalDate.of(2026, 7, 10),
            new BigDecimal("250.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "PARTIAL-1",
            null
        );

        var first = service.schedule(obligation.getId()).getFirst();
        assertThat(first.getStatus()).isEqualTo(InstallmentStatus.PARTIALLY_PAID);
        assertThat(first.getPaidAmount()).isEqualByComparingTo("250.00");
        assertThat(first.remainingAmount()).isEqualByComparingTo("250.00");
    }

    @Test
    void paymentSettlesOldestInstallmentsBeforeNewerOnes() {
        var obligation = structuredObligation();

        service.pay(
            obligation.getId(),
            LocalDate.of(2026, 7, 10),
            new BigDecimal("650.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "PAY-2",
            null
        );

        var schedule = service.schedule(obligation.getId());
        assertThat(schedule.get(0).remainingAmount()).isZero();
        assertThat(schedule.get(0).getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(schedule.get(1).remainingAmount()).isEqualByComparingTo("350.00");
        assertThat(schedule.get(1).getStatus()).isEqualTo(InstallmentStatus.PARTIALLY_PAID);
    }

    private dev.harrison.rendacomcarro.finance.domain.FinancialObligation structuredObligation() {
        return service.create(new FinancialObligationService.CreateCommand(
            null,
            ObligationType.VEHICLE_FINANCING,
            ObligationMode.STRUCTURED,
            "Banco",
            new BigDecimal("1000.00"),
            BigDecimal.ZERO,
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 7, 5),
            2,
            new BigDecimal("500.00"),
            null,
            null
        ));
    }
}
