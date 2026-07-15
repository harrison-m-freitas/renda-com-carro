package dev.harrison.rendacomcarro.finance;

import static org.assertj.core.api.Assertions.assertThat;

import dev.harrison.rendacomcarro.finance.domain.InterestRatePeriod;
import dev.harrison.rendacomcarro.finance.domain.ObligationCalculationMethod;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.domain.ObligationType;
import dev.harrison.rendacomcarro.finance.web.ObligationForm;
import dev.harrison.rendacomcarro.finance.web.ObligationFormValidator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;

class ObligationFormValidatorTest {
    private final ObligationFormValidator validator = new ObligationFormValidator();

    @Test
    void fixedInstallmentKnownRequiresTheInstallmentAndChronologicalDueDate() {
        ObligationForm form = baseForm();
        form.setMode(ObligationMode.FIXED_INSTALLMENTS);
        form.setCalculationMethod(ObligationCalculationMethod.INSTALLMENT_KNOWN);
        form.setTermMonths(36);
        form.setFirstDueDate(LocalDate.of(2026, 6, 1));

        BeanPropertyBindingResult errors = validate(form);

        assertThat(errors.getFieldError("installmentAmount")).isNotNull();
        assertThat(errors.getFieldError("firstDueDate")).isNotNull();
    }

    @Test
    void rateKnownRequiresPeriodAndNonNegativeRate() {
        ObligationForm form = baseForm();
        form.setMode(ObligationMode.FIXED_INSTALLMENTS);
        form.setCalculationMethod(ObligationCalculationMethod.RATE_KNOWN);
        form.setTermMonths(12);
        form.setFirstDueDate(LocalDate.of(2026, 8, 14));
        form.setInterestRatePercent(new BigDecimal("-1"));
        form.setInterestRatePeriod(null);

        BeanPropertyBindingResult errors = validate(form);

        assertThat(errors.getFieldError("interestRatePercent")).isNotNull();
        assertThat(errors.getFieldError("interestRatePeriod")).isNotNull();
    }

    @Test
    void flexibleUnknownRateRequiresOnlyPositiveMonthlyTarget() {
        ObligationForm form = baseForm();
        form.setMode(ObligationMode.FLEXIBLE_PAYMENTS);
        form.setCalculationMethod(ObligationCalculationMethod.RATE_UNKNOWN);
        form.setMonthlyTarget(new BigDecimal("500.00"));
        form.setInstallmentAmount(new BigDecimal("999.00"));

        BeanPropertyBindingResult errors = validate(form);

        assertThat(errors.hasErrors()).isFalse();
    }

    @Test
    void singlePaymentMustNotBeBelowTheAmountReceived() {
        ObligationForm form = baseForm();
        form.setMode(ObligationMode.SINGLE_PAYMENT);
        form.setFirstDueDate(LocalDate.of(2026, 8, 14));
        form.setSinglePaymentAmount(new BigDecimal("9000.00"));

        BeanPropertyBindingResult errors = validate(form);

        assertThat(errors.getFieldError("singlePaymentAmount")).isNotNull();
    }

    private BeanPropertyBindingResult validate(ObligationForm form) {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "obligationForm");
        validator.validate(form, errors);
        return errors;
    }

    private ObligationForm baseForm() {
        ObligationForm form = new ObligationForm();
        form.setDraftKey("draft:3af5236e-0ef6-4591-a2c2-c1a33db89f08");
        form.setType(ObligationType.BANK_FINANCING);
        form.setCreditor("Banco");
        form.setPrincipalAmount(new BigDecimal("10000.00"));
        form.setStartDate(LocalDate.of(2026, 7, 14));
        form.setInterestRatePeriod(InterestRatePeriod.MONTHLY);
        return form;
    }
}
