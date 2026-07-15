package dev.harrison.rendacomcarro.draft.application.definition;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.application.DraftPayloadValidator;
import dev.harrison.rendacomcarro.draft.application.FormDraftDefinition;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.finance.domain.InterestRatePeriod;
import dev.harrison.rendacomcarro.finance.domain.ObligationCalculationMethod;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.domain.ObligationType;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ObligationDraftDefinition implements FormDraftDefinition {
    private static final Pattern KEY_PATTERN = Pattern.compile("^draft:([0-9a-fA-F-]{36})$");
    private static final Set<String> ALLOWED_FIELDS = Set.of(
        "acquisitionPlanId", "vehicleId", "creditor", "type", "mode",
        "calculationMethod", "principalAmount", "interestRatePercent",
        "interestRatePeriod", "startDate", "firstDueDate", "termMonths",
        "installmentAmount", "singlePaymentAmount", "monthlyTarget", "notes"
    );
    private static final Set<String> TEXT_FIELDS = Set.of("creditor", "notes");

    private final DraftPayloadValidator validator;

    public ObligationDraftDefinition(DraftPayloadValidator validator) {
        this.validator = validator;
    }

    @Override public FormDraftType type() { return FormDraftType.OBLIGATION; }
    @Override public int schemaVersion() { return 2; }
    @Override public int maxStep() { return 4; }

    @Override
    public String normalizeContextKey(String contextKey) {
        String normalized = contextKey == null ? "" : contextKey.trim();
        Matcher matcher = KEY_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            throw new DomainValidationException("A chave do rascunho de obrigação é inválida.");
        }
        try {
            UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException("A chave do rascunho de obrigação é inválida.");
        }
        return normalized;
    }

    @Override
    public ObjectNode normalizeAndValidate(
        ObjectNode payload,
        int currentStep,
        boolean validateCurrentStep
    ) {
        validator.rejectUnknownFields(payload, ALLOWED_FIELDS);
        ObjectNode normalized = validator.sanitizeTextFields(payload, TEXT_FIELDS);
        normalizeOptionalUuid(normalized, "acquisitionPlanId", "Plano de compra");
        normalizeOptionalUuid(normalized, "vehicleId", "Veículo");

        boolean step1 = validateCurrentStep && currentStep >= 1;
        boolean step2 = validateCurrentStep && currentStep >= 2;
        boolean step3 = validateCurrentStep && currentStep >= 3;

        normalizeRequiredText(normalized, "creditor", "Credor", step1);
        ObligationType type = enumValue(
            normalized, "type", "Tipo", ObligationType.class, step1
        );
        if (type != null) normalized.put("type", type.name());

        ObligationMode mode = enumValue(
            normalized, "mode", "Forma de pagamento", ObligationMode.class, step2
        );
        if (mode != null) normalized.put("mode", mode.name());

        ObligationCalculationMethod method = enumValue(
            normalized, "calculationMethod", "Informações conhecidas",
            ObligationCalculationMethod.class, step3
        );
        if (method != null) normalized.put("calculationMethod", method.name());

        normalizePositiveDecimal(
            normalized, "principalAmount", "Valor emprestado ou financiado", step2
        );
        LocalDate startDate = normalizeDate(
            normalized, "startDate", "Data do contrato", step2
        );

        if (mode == null || method == null) {
            return normalized;
        }
        switch (mode) {
            case FIXED_INSTALLMENTS -> normalizeFixed(normalized, method, startDate, step3);
            case FLEXIBLE_PAYMENTS -> normalizeFlexible(normalized, method, step3);
            case SINGLE_PAYMENT -> normalizeSingle(normalized, method, startDate, step3);
        }
        return normalized;
    }

    private void normalizeFixed(
        ObjectNode payload,
        ObligationCalculationMethod method,
        LocalDate startDate,
        boolean required
    ) {
        LocalDate due = normalizeDate(payload, "firstDueDate", "Primeiro vencimento", required);
        validateChronology(startDate, due);
        Integer term = validator.optionalInteger(payload, "termMonths", "Parcelas");
        if (term == null && required) {
            throw new DomainValidationException("A quantidade de parcelas é obrigatória.");
        }
        if (term != null) {
            if (term < 1 || term > 600) {
                throw new DomainValidationException("Informe de 1 a 600 parcelas.");
            }
            payload.put("termMonths", term);
        } else {
            payload.remove("termMonths");
        }
        switch (method) {
            case INSTALLMENT_KNOWN -> {
                normalizePositiveDecimal(
                    payload, "installmentAmount", "Valor da parcela", required
                );
                payload.remove("interestRatePercent");
                payload.remove("interestRatePeriod");
            }
            case RATE_KNOWN -> {
                normalizeRate(payload, required);
                payload.remove("installmentAmount");
            }
            case INTEREST_FREE -> {
                payload.remove("installmentAmount");
                payload.remove("interestRatePercent");
                payload.remove("interestRatePeriod");
            }
            default -> throw new DomainValidationException(
                "A forma de cálculo não é válida para parcelas fixas."
            );
        }
        payload.remove("monthlyTarget");
        payload.remove("singlePaymentAmount");
    }

    private void normalizeFlexible(
        ObjectNode payload,
        ObligationCalculationMethod method,
        boolean required
    ) {
        payload.remove("firstDueDate");
        payload.remove("termMonths");
        payload.remove("installmentAmount");
        payload.remove("singlePaymentAmount");
        normalizePositiveDecimal(payload, "monthlyTarget", "Pagamento mensal planejado", required);
        switch (method) {
            case RATE_KNOWN -> normalizeRate(payload, required);
            case INTEREST_FREE, RATE_UNKNOWN -> {
                payload.remove("interestRatePercent");
                payload.remove("interestRatePeriod");
            }
            default -> throw new DomainValidationException(
                "A forma de cálculo não é válida para pagamentos livres."
            );
        }
    }

    private void normalizeSingle(
        ObjectNode payload,
        ObligationCalculationMethod method,
        LocalDate startDate,
        boolean required
    ) {
        payload.remove("termMonths");
        payload.remove("installmentAmount");
        payload.remove("monthlyTarget");
        payload.remove("interestRatePercent");
        payload.remove("interestRatePeriod");
        LocalDate due = normalizeDate(payload, "firstDueDate", "Data do pagamento", required);
        validateChronology(startDate, due);
        if (method == ObligationCalculationMethod.TOTAL_KNOWN) {
            normalizePositiveDecimal(
                payload, "singlePaymentAmount", "Valor total a pagar", required
            );
        } else if (method == ObligationCalculationMethod.INTEREST_FREE) {
            payload.remove("singlePaymentAmount");
        } else {
            throw new DomainValidationException(
                "A forma de cálculo não é válida para pagamento único."
            );
        }
    }

    private void normalizeRate(ObjectNode payload, boolean required) {
        BigDecimal rate = required
            ? validator.requireDecimal(payload, "interestRatePercent", "Taxa de juros")
            : validator.optionalDecimal(payload, "interestRatePercent", "Taxa de juros");
        if (rate == null) {
            payload.remove("interestRatePercent");
        } else {
            if (rate.signum() < 0 || rate.compareTo(new BigDecimal("10000")) > 0) {
                throw new DomainValidationException("A taxa deve estar entre 0 e 10.000%.");
            }
            payload.put("interestRatePercent", rate.toPlainString());
        }
        InterestRatePeriod period = enumValue(
            payload, "interestRatePeriod", "Periodicidade da taxa",
            InterestRatePeriod.class, required
        );
        if (period != null) payload.put("interestRatePeriod", period.name());
    }

    private void normalizeOptionalUuid(ObjectNode payload, String field, String label) {
        UUID value = validator.optionalUuid(payload, field, label);
        if (value == null) payload.remove(field); else payload.put(field, value.toString());
    }

    private void normalizeRequiredText(
        ObjectNode payload,
        String field,
        String label,
        boolean required
    ) {
        String value = required
            ? validator.requireText(payload, field, label)
            : validator.optionalText(payload, field);
        if (value == null) payload.remove(field); else payload.put(field, value);
    }

    private BigDecimal normalizePositiveDecimal(
        ObjectNode payload,
        String field,
        String label,
        boolean required
    ) {
        BigDecimal value = required
            ? validator.requireDecimal(payload, field, label)
            : validator.optionalDecimal(payload, field, label);
        if (value == null) {
            payload.remove(field);
            return null;
        }
        if (value.signum() <= 0) {
            throw new DomainValidationException(label + " deve ser maior que zero.");
        }
        payload.put(field, value.toPlainString());
        return value;
    }

    private LocalDate normalizeDate(
        ObjectNode payload,
        String field,
        String label,
        boolean required
    ) {
        LocalDate value = required
            ? validator.requireDate(payload, field, label)
            : validator.optionalDate(payload, field, label);
        if (value == null) payload.remove(field); else payload.put(field, value.toString());
        return value;
    }

    private <E extends Enum<E>> E enumValue(
        ObjectNode payload,
        String field,
        String label,
        Class<E> enumType,
        boolean required
    ) {
        String raw = required
            ? validator.requireText(payload, field, label)
            : validator.optionalText(payload, field);
        if (raw == null) {
            payload.remove(field);
            return null;
        }
        try {
            return Enum.valueOf(enumType, raw);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException(label + " é inválido.");
        }
    }

    private void validateChronology(LocalDate startDate, LocalDate dueDate) {
        if (startDate != null && dueDate != null && dueDate.isBefore(startDate)) {
            throw new DomainValidationException(
                "O vencimento não pode ser anterior à data do contrato."
            );
        }
    }

}
