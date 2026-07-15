package dev.harrison.rendacomcarro.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.harrison.rendacomcarro.finance.application.InstallmentScheduleCalculator;
import dev.harrison.rendacomcarro.finance.application.ObligationCalculationException;
import dev.harrison.rendacomcarro.finance.domain.InterestRatePeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InstallmentScheduleCalculatorTest {
    private final InstallmentScheduleCalculator calculator = new InstallmentScheduleCalculator();
    private final LocalDate firstDueDate = LocalDate.of(2026, 8, 10);

    @Test
    void interestFreeSchedulePreservesPrincipalAndAdjustsOnlyTheLastRoundingDifference() {
        var schedule = calculator.calculateInterestFree(
            new BigDecimal("10000.00"),
            3,
            firstDueDate
        );

        assertThat(schedule.entries()).hasSize(3);
        assertThat(schedule.entries().get(0).total()).isEqualByComparingTo("3333.33");
        assertThat(schedule.entries().get(1).total()).isEqualByComparingTo("3333.33");
        assertThat(schedule.entries().get(2).total()).isEqualByComparingTo("3333.34");
        assertThat(schedule.installmentAmount()).isEqualByComparingTo("3333.33");
        assertThat(schedule.totalAmount()).isEqualByComparingTo("10000.00");
        assertThat(schedule.totalInterest()).isEqualByComparingTo("0.00");
        assertThat(schedule.monthlyRate()).isEqualByComparingTo("0");
    }

    @Test
    void interestFreeScheduleNeverCreatesAZeroInstallmentWhenCentsCanBeDistributed() {
        var schedule = calculator.calculateInterestFree(
            new BigDecimal("1.00"),
            18,
            firstDueDate
        );

        assertThat(schedule.entries()).hasSize(18);
        assertThat(schedule.entries())
            .allSatisfy(entry -> assertThat(entry.total()).isGreaterThan(BigDecimal.ZERO));
        assertThat(schedule.entries().stream()
            .map(InstallmentScheduleCalculator.ScheduleEntry::total)
            .reduce(BigDecimal.ZERO, BigDecimal::add))
            .isEqualByComparingTo("1.00");
        assertThat(schedule.entries().stream().filter(
            entry -> entry.total().compareTo(new BigDecimal("0.06")) == 0
        )).hasSize(10);
        assertThat(schedule.entries().stream().filter(
            entry -> entry.total().compareTo(new BigDecimal("0.05")) == 0
        )).hasSize(8);
    }

    @Test
    void knownInstallmentInfersTheRateAndUsesTheContractualInstallment() {
        var schedule = calculator.calculateFromInstallment(
            new BigDecimal("35000.00"),
            new BigDecimal("1386.00"),
            36,
            firstDueDate
        );

        assertThat(schedule.entries()).hasSize(36);
        assertThat(schedule.installmentAmount()).isEqualByComparingTo("1386.00");
        assertThat(schedule.entries().subList(0, 35))
            .allSatisfy(entry -> assertThat(entry.total()).isEqualByComparingTo("1386.00"));
        assertThat(schedule.monthlyRate().movePointRight(2).doubleValue())
            .isBetween(2.05, 2.08);
        assertThat(schedule.annualEffectiveRate().movePointRight(2).doubleValue())
            .isBetween(27.5, 28.0);
        assertThat(schedule.totalAmount()).isGreaterThan(new BigDecimal("49000.00"));
        assertThat(schedule.totalInterest()).isEqualByComparingTo(
            schedule.totalAmount().subtract(new BigDecimal("35000.00"))
        );
        assertThat(schedule.lastDueDate()).isEqualTo(LocalDate.of(2029, 7, 10));
    }

    @Test
    void knownAnnualEffectiveRateIsConvertedBeforeCalculatingTheInstallment() {
        var schedule = calculator.calculateFromRate(
            new BigDecimal("12000.00"),
            new BigDecimal("0.12"),
            InterestRatePeriod.ANNUAL,
            12,
            firstDueDate
        );

        assertThat(schedule.annualEffectiveRate()).isCloseTo(
            new BigDecimal("0.12"),
            org.assertj.core.data.Offset.offset(new BigDecimal("0.000001"))
        );
        assertThat(schedule.monthlyRate()).isGreaterThan(BigDecimal.ZERO);
        assertThat(schedule.installmentAmount()).isGreaterThan(new BigDecimal("1000.00"));
        assertThat(schedule.totalInterest()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void installmentBelowTheInterestFreeMinimumIsRejectedOnTheInstallmentField() {
        assertThatThrownBy(() -> calculator.calculateFromInstallment(
            new BigDecimal("10000.00"),
            new BigDecimal("800.00"),
            12,
            firstDueDate
        ))
            .isInstanceOf(ObligationCalculationException.class)
            .satisfies(error -> assertThat(
                ((ObligationCalculationException) error).field()
            ).isEqualTo("installmentAmount"))
            .hasMessageContaining("não quita");
    }

    @Test
    void singlePaymentSeparatesPrincipalAndKnownFinancingCost() {
        var schedule = calculator.calculateSinglePayment(
            new BigDecimal("10000.00"),
            new BigDecimal("11200.00"),
            firstDueDate
        );

        assertThat(schedule.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.principal()).isEqualByComparingTo("10000.00");
            assertThat(entry.interest()).isEqualByComparingTo("1200.00");
            assertThat(entry.total()).isEqualByComparingTo("11200.00");
        });
        assertThat(schedule.totalInterest()).isEqualByComparingTo("1200.00");
    }

    @Test
    void flexibleEstimateWarnsWhenTheMonthlyTargetDoesNotCoverInterest() {
        var estimate = calculator.estimateFlexible(
            new BigDecimal("10000.00"),
            new BigDecimal("100.00"),
            new BigDecimal("0.02")
        );

        assertThat(estimate.amortizes()).isFalse();
        assertThat(estimate.estimatedMonths()).isNull();
        assertThat(estimate.firstMonthInterest()).isEqualByComparingTo("200.00");
    }
}
