package dev.harrison.rendacomcarro.finance.application;

import dev.harrison.rendacomcarro.finance.domain.InterestRatePeriod;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InstallmentScheduleCalculator {
    private static final MathContext CALCULATION_CONTEXT = new MathContext(24, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_CENT_HALF = new BigDecimal("0.005");
    private static final BigDecimal MAX_INFERRED_MONTHLY_RATE = new BigDecimal("1024");
    private static final int RATE_SEARCH_ITERATIONS = 220;
    private static final int MAX_FLEXIBLE_MONTHS = 1_200;

    public record ScheduleEntry(
        int sequence,
        LocalDate dueDate,
        BigDecimal principal,
        BigDecimal interest,
        BigDecimal total
    ) {}

    public record RepaymentSchedule(
        List<ScheduleEntry> entries,
        BigDecimal monthlyRate,
        BigDecimal annualEffectiveRate,
        BigDecimal installmentAmount,
        BigDecimal totalAmount,
        BigDecimal totalInterest,
        LocalDate lastDueDate
    ) {}

    public record FlexibleEstimate(
        Integer estimatedMonths,
        BigDecimal firstMonthInterest,
        boolean amortizes
    ) {}

    public RepaymentSchedule calculateFromRate(
        BigDecimal principal,
        BigDecimal rateRatio,
        InterestRatePeriod period,
        int installments,
        LocalDate firstDueDate
    ) {
        validateCommon(principal, installments, firstDueDate);
        if (rateRatio == null || rateRatio.signum() < 0) {
            throw error("interestRatePercent", "A taxa de juros não pode ser negativa.");
        }
        if (period == null) {
            throw error("interestRatePeriod", "Informe se a taxa é mensal ou anual.");
        }

        BigDecimal monthlyRate = period == InterestRatePeriod.MONTHLY
            ? rateRatio
            : effectiveAnnualToMonthly(rateRatio);
        BigDecimal installmentAmount = money(paymentForRate(principal, monthlyRate, installments));
        return buildSchedule(
            principal,
            monthlyRate,
            installments,
            firstDueDate,
            installmentAmount
        );
    }

    public RepaymentSchedule calculateFromInstallment(
        BigDecimal principal,
        BigDecimal installmentAmount,
        int installments,
        LocalDate firstDueDate
    ) {
        validateCommon(principal, installments, firstDueDate);
        requirePositive(installmentAmount, "installmentAmount", "Informe o valor da parcela.");

        BigDecimal minimumPayment = principal.divide(
            BigDecimal.valueOf(installments),
            CALCULATION_CONTEXT
        );
        if (installmentAmount.add(ONE_CENT_HALF).compareTo(minimumPayment) < 0) {
            throw error(
                "installmentAmount",
                "A parcela informada não quita o valor financiado dentro desse prazo."
            );
        }

        BigDecimal monthlyRate = installmentAmount.subtract(minimumPayment).abs()
            .compareTo(ONE_CENT_HALF) < 0
            ? BigDecimal.ZERO
            : inferMonthlyRate(principal, installmentAmount, installments);

        return buildSchedule(
            principal,
            monthlyRate,
            installments,
            firstDueDate,
            money(installmentAmount)
        );
    }

    public RepaymentSchedule calculateInterestFree(
        BigDecimal principal,
        int installments,
        LocalDate firstDueDate
    ) {
        validateCommon(principal, installments, firstDueDate);
        BigDecimal installmentAmount = money(
            principal.divide(BigDecimal.valueOf(installments), CALCULATION_CONTEXT)
        );
        return buildSchedule(
            principal,
            BigDecimal.ZERO,
            installments,
            firstDueDate,
            installmentAmount
        );
    }

    public RepaymentSchedule calculateSinglePayment(
        BigDecimal principal,
        BigDecimal totalPayment,
        LocalDate dueDate
    ) {
        requirePositive(principal, "principalAmount", "Informe o valor recebido.");
        requirePositive(totalPayment, "singlePaymentAmount", "Informe o valor do pagamento.");
        if (dueDate == null) {
            throw error("firstDueDate", "Informe a data do pagamento.");
        }
        BigDecimal normalizedPrincipal = money(principal);
        BigDecimal normalizedTotal = money(totalPayment);
        if (normalizedTotal.compareTo(normalizedPrincipal) < 0) {
            throw error(
                "singlePaymentAmount",
                "O valor a pagar não pode ser menor que o valor recebido."
            );
        }
        BigDecimal cost = money(normalizedTotal.subtract(normalizedPrincipal));
        return new RepaymentSchedule(
            List.of(new ScheduleEntry(1, dueDate, normalizedPrincipal, cost, normalizedTotal)),
            null,
            null,
            normalizedTotal,
            normalizedTotal,
            cost,
            dueDate
        );
    }

    public FlexibleEstimate estimateFlexible(
        BigDecimal principal,
        BigDecimal monthlyTarget,
        BigDecimal monthlyRate
    ) {
        requirePositive(principal, "principalAmount", "Informe o valor recebido.");
        requirePositive(monthlyTarget, "monthlyTarget", "Informe quanto pretende pagar por mês.");
        if (monthlyRate == null || monthlyRate.signum() < 0) {
            throw error("interestRatePercent", "A taxa de juros não pode ser negativa.");
        }

        BigDecimal balance = money(principal);
        BigDecimal firstMonthInterest = money(balance.multiply(monthlyRate, CALCULATION_CONTEXT));
        if (monthlyRate.signum() > 0 && monthlyTarget.compareTo(firstMonthInterest) <= 0) {
            return new FlexibleEstimate(null, firstMonthInterest, false);
        }

        int months = 0;
        while (balance.signum() > 0 && months < MAX_FLEXIBLE_MONTHS) {
            BigDecimal interest = money(balance.multiply(monthlyRate, CALCULATION_CONTEXT));
            balance = money(balance.add(interest).subtract(monthlyTarget));
            if (balance.signum() < 0) {
                balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            months++;
        }
        return new FlexibleEstimate(
            balance.signum() == 0 ? months : null,
            firstMonthInterest,
            balance.signum() == 0
        );
    }

    public BigDecimal normalizeMonthlyRate(
        BigDecimal rateRatio,
        InterestRatePeriod period
    ) {
        if (rateRatio == null) return null;
        if (period == null) {
            throw error("interestRatePeriod", "Informe se a taxa é mensal ou anual.");
        }
        return period == InterestRatePeriod.MONTHLY
            ? rateRatio
            : effectiveAnnualToMonthly(rateRatio);
    }

    public BigDecimal annualEffectiveRate(BigDecimal monthlyRate) {
        if (monthlyRate == null) return null;
        return BigDecimal.ONE.add(monthlyRate, CALCULATION_CONTEXT)
            .pow(12, CALCULATION_CONTEXT)
            .subtract(BigDecimal.ONE, CALCULATION_CONTEXT)
            .setScale(12, RoundingMode.HALF_UP);
    }

    private RepaymentSchedule buildSchedule(
        BigDecimal principal,
        BigDecimal monthlyRate,
        int installments,
        LocalDate firstDueDate,
        BigDecimal contractualInstallment
    ) {
        BigDecimal balance = money(principal);
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        List<ScheduleEntry> entries = new ArrayList<>(installments);

        for (int sequence = 1; sequence <= installments; sequence++) {
            BigDecimal interest = money(balance.multiply(monthlyRate, CALCULATION_CONTEXT));
            BigDecimal principalPart;
            BigDecimal total;

            if (sequence == installments) {
                principalPart = balance;
                total = money(principalPart.add(interest));
            } else {
                principalPart = money(contractualInstallment.subtract(interest));
                if (principalPart.signum() <= 0) {
                    throw error(
                        "installmentAmount",
                        "A parcela informada não é suficiente para amortizar a dívida."
                    );
                }
                if (principalPart.compareTo(balance) > 0) {
                    principalPart = balance;
                }
                total = money(principalPart.add(interest));
            }

            entries.add(new ScheduleEntry(
                sequence,
                firstDueDate.plusMonths(sequence - 1L),
                principalPart,
                interest,
                total
            ));
            balance = money(balance.subtract(principalPart));
            totalAmount = money(totalAmount.add(total));
        }

        BigDecimal totalInterest = money(totalAmount.subtract(money(principal)));
        return new RepaymentSchedule(
            List.copyOf(entries),
            monthlyRate.setScale(12, RoundingMode.HALF_UP),
            annualEffectiveRate(monthlyRate),
            money(contractualInstallment),
            totalAmount,
            totalInterest,
            firstDueDate.plusMonths(installments - 1L)
        );
    }

    private BigDecimal inferMonthlyRate(
        BigDecimal principal,
        BigDecimal installmentAmount,
        int installments
    ) {
        BigDecimal low = BigDecimal.ZERO;
        BigDecimal high = BigDecimal.ONE;
        while (paymentForRate(principal, high, installments).compareTo(installmentAmount) < 0
            && high.compareTo(MAX_INFERRED_MONTHLY_RATE) < 0) {
            high = high.multiply(BigDecimal.valueOf(2), CALCULATION_CONTEXT);
        }
        if (paymentForRate(principal, high, installments).compareTo(installmentAmount) < 0) {
            throw error(
                "installmentAmount",
                "Não foi possível encontrar uma taxa compatível com a parcela informada."
            );
        }

        for (int index = 0; index < RATE_SEARCH_ITERATIONS; index++) {
            BigDecimal middle = low.add(high, CALCULATION_CONTEXT)
                .divide(BigDecimal.valueOf(2), CALCULATION_CONTEXT);
            if (paymentForRate(principal, middle, installments)
                .compareTo(installmentAmount) < 0) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low.add(high, CALCULATION_CONTEXT)
            .divide(BigDecimal.valueOf(2), CALCULATION_CONTEXT)
            .setScale(12, RoundingMode.HALF_UP);
    }

    private BigDecimal paymentForRate(
        BigDecimal principal,
        BigDecimal monthlyRate,
        int installments
    ) {
        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(installments), CALCULATION_CONTEXT);
        }
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate, CALCULATION_CONTEXT)
            .pow(installments, CALCULATION_CONTEXT);
        return principal.multiply(monthlyRate, CALCULATION_CONTEXT)
            .multiply(factor, CALCULATION_CONTEXT)
            .divide(factor.subtract(BigDecimal.ONE, CALCULATION_CONTEXT), CALCULATION_CONTEXT);
    }

    private BigDecimal effectiveAnnualToMonthly(BigDecimal annualRate) {
        double monthly = Math.pow(1.0d + annualRate.doubleValue(), 1.0d / 12.0d) - 1.0d;
        if (!Double.isFinite(monthly) || monthly < 0) {
            throw error("interestRatePercent", "A taxa anual informada é inválida.");
        }
        return BigDecimal.valueOf(monthly).setScale(12, RoundingMode.HALF_UP);
    }

    private void validateCommon(
        BigDecimal principal,
        int installments,
        LocalDate firstDueDate
    ) {
        requirePositive(principal, "principalAmount", "Informe o valor recebido.");
        if (installments < 1 || installments > 600) {
            throw error("termMonths", "Informe de 1 a 600 parcelas.");
        }
        if (firstDueDate == null) {
            throw error("firstDueDate", "Informe o primeiro vencimento.");
        }
    }

    private void requirePositive(BigDecimal value, String field, String message) {
        if (value == null || value.signum() <= 0) {
            throw error(field, message);
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private ObligationCalculationException error(String field, String message) {
        return new ObligationCalculationException(field, message);
    }
}
