package dev.harrison.rendacomcarro.finance.web;

import dev.harrison.rendacomcarro.finance.domain.ObligationCalculationMethod;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class ObligationFormValidator implements Validator {
    private static final BigDecimal MAX_RATE_PERCENT = new BigDecimal("10000");

    @Override
    public boolean supports(Class<?> clazz) {
        return ObligationForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ObligationForm form = (ObligationForm) target;
        if (form.getMode() == null || form.getCalculationMethod() == null) {
            return;
        }
        validateChronology(form, errors);
        switch (form.getMode()) {
            case FIXED_INSTALLMENTS -> validateFixed(form, errors);
            case FLEXIBLE_PAYMENTS -> validateFlexible(form, errors);
            case SINGLE_PAYMENT -> validateSingle(form, errors);
        }
    }

    private void validateFixed(ObligationForm form, Errors errors) {
        if (form.getFirstDueDate() == null) {
            errors.rejectValue("firstDueDate", "required", "Informe o primeiro vencimento");
        }
        if (form.getTermMonths() == null || form.getTermMonths() < 1 || form.getTermMonths() > 600) {
            errors.rejectValue("termMonths", "range", "Informe de 1 a 600 parcelas");
        }
        switch (form.getCalculationMethod()) {
            case INSTALLMENT_KNOWN -> requirePositive(
                errors, "installmentAmount", form.getInstallmentAmount(), "Informe o valor da parcela"
            );
            case RATE_KNOWN -> validateRate(form, errors);
            case INTEREST_FREE -> { }
            default -> errors.rejectValue(
                "calculationMethod", "invalid", "Escolha parcela conhecida, taxa conhecida ou sem juros"
            );
        }
    }

    private void validateFlexible(ObligationForm form, Errors errors) {
        requirePositive(
            errors, "monthlyTarget", form.getMonthlyTarget(), "Informe quanto pretende pagar por mês"
        );
        switch (form.getCalculationMethod()) {
            case RATE_KNOWN -> validateRate(form, errors);
            case INTEREST_FREE, RATE_UNKNOWN -> { }
            default -> errors.rejectValue(
                "calculationMethod", "invalid", "Escolha taxa conhecida, sem juros ou taxa desconhecida"
            );
        }
    }

    private void validateSingle(ObligationForm form, Errors errors) {
        if (form.getFirstDueDate() == null) {
            errors.rejectValue("firstDueDate", "required", "Informe a data do pagamento");
        }
        if (form.getCalculationMethod() == ObligationCalculationMethod.TOTAL_KNOWN) {
            requirePositive(
                errors, "singlePaymentAmount", form.getSinglePaymentAmount(),
                "Informe o valor total que será pago"
            );
            if (form.getPrincipalAmount() != null && form.getSinglePaymentAmount() != null
                && form.getSinglePaymentAmount().compareTo(form.getPrincipalAmount()) < 0) {
                errors.rejectValue(
                    "singlePaymentAmount", "minimum",
                    "O valor a pagar não pode ser menor que o valor recebido"
                );
            }
        } else if (form.getCalculationMethod() != ObligationCalculationMethod.INTEREST_FREE) {
            errors.rejectValue(
                "calculationMethod", "invalid", "Escolha valor total conhecido ou pagamento sem juros"
            );
        }
    }

    private void validateRate(ObligationForm form, Errors errors) {
        BigDecimal rate = form.getInterestRatePercent();
        if (rate == null) {
            errors.rejectValue("interestRatePercent", "required", "Informe a taxa de juros");
        } else if (rate.signum() < 0 || rate.compareTo(MAX_RATE_PERCENT) > 0) {
            errors.rejectValue(
                "interestRatePercent", "range", "Informe uma taxa entre 0 e 10.000%"
            );
        }
        if (form.getInterestRatePeriod() == null) {
            errors.rejectValue(
                "interestRatePeriod", "required", "Informe se a taxa é mensal ou anual"
            );
        }
    }

    private void validateChronology(ObligationForm form, Errors errors) {
        if (form.getStartDate() != null && form.getFirstDueDate() != null
            && form.getFirstDueDate().isBefore(form.getStartDate())) {
            errors.rejectValue(
                "firstDueDate", "chronology",
                "O vencimento não pode ser anterior à data do contrato"
            );
        }
    }

    private void requirePositive(Errors errors, String field, BigDecimal value, String message) {
        if (value == null || value.signum() <= 0) {
            errors.rejectValue(field, "positive", message);
        }
    }
}
