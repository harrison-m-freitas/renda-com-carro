package dev.harrison.rendacomcarro.draft.application.definition;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.application.DraftPayloadValidator;
import dev.harrison.rendacomcarro.draft.application.FormDraftDefinition;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ExpenseDraftDefinition implements FormDraftDefinition {
    private static final Pattern SESSION_KEY = Pattern.compile(
        "^expense:new:([0-9a-fA-F-]{36})$"
    );
    private static final Set<String> ALLOWED_FIELDS = Set.of(
        "vehicleId", "operationalDayId", "shiftId", "categoryId",
        "expenseDate", "competenceMonth", "paymentStatus", "paidDate",
        "amount", "classification", "allocationMethod",
        "professionalPercentagePercent", "professionalFixedAmount",
        "adjustmentReason", "notes"
    );
    private static final Set<String> TEXT_FIELDS = Set.of("adjustmentReason", "notes");

    private final DraftPayloadValidator validator;

    public ExpenseDraftDefinition(DraftPayloadValidator validator) {
        this.validator = validator;
    }

    @Override public FormDraftType type() { return FormDraftType.EXPENSE; }
    @Override public int schemaVersion() { return 2; }
    @Override public int maxStep() { return 3; }

    @Override
    public ObjectNode migrate(int sourceSchemaVersion, ObjectNode payload) {
        if (sourceSchemaVersion == schemaVersion()) {
            return payload.deepCopy();
        }
        if (sourceSchemaVersion != 1) {
            throw new DomainValidationException("Versão de rascunho incompatível.");
        }
        ObjectNode migrated = payload.deepCopy();
        String paidDate = validator.optionalText(migrated, "paidDate");
        migrated.put("paymentStatus", paidDate == null ? "PENDING" : "PAID");
        return migrated;
    }

    @Override
    public String normalizeContextKey(String contextKey) {
        String normalized = contextKey == null ? "" : contextKey.trim();
        if ("current".equals(normalized)) {
            return normalized;
        }
        Matcher matcher = SESSION_KEY.matcher(normalized);
        if (!matcher.matches()) {
            throw new DomainValidationException("A chave do rascunho de gasto é inválida.");
        }
        try {
            UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException("A chave do rascunho de gasto é inválida.");
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

        normalizeUuid(normalized, "vehicleId", "Veículo", validateCurrentStep && currentStep >= 1);
        normalizeUuid(normalized, "operationalDayId", "Dia operacional", false);
        normalizeUuid(normalized, "shiftId", "Turno", false);
        normalizeUuid(normalized, "categoryId", "Categoria", validateCurrentStep && currentStep >= 1);
        normalizeDate(normalized, "expenseDate", "Data do gasto", validateCurrentStep && currentStep >= 1);
        normalizePositiveDecimal(normalized, "amount", "Valor", validateCurrentStep && currentStep >= 1);

        normalizeMonth(
            normalized,
            "competenceMonth",
            "Mês de referência",
            validateCurrentStep && currentStep >= 2
        );
        normalizePayment(
            normalized,
            validateCurrentStep && currentStep >= 2
        );

        ExpenseClassification classification = optionalClassification(normalized);
        if (classification == null && validateCurrentStep && currentStep >= 2) {
            classification = requireClassification(normalized);
        }
        if (classification == null) {
            return normalized;
        }
        normalized.put("classification", classification.name());

        if (classification != ExpenseClassification.MIXED) {
            normalized.remove("allocationMethod");
            normalized.remove("professionalPercentagePercent");
            normalized.remove("professionalFixedAmount");
            normalized.remove("adjustmentReason");
            return normalized;
        }

        AllocationMethod method = optionalAllocationMethod(normalized);
        if (method == null && validateCurrentStep && currentStep >= 2) {
            method = requireAllocationMethod(normalized);
        }
        if (method == null) {
            return normalized;
        }
        normalized.put("allocationMethod", method.name());

        switch (method) {
            case MILEAGE_RATIO -> {
                normalized.remove("professionalPercentagePercent");
                normalized.remove("professionalFixedAmount");
                normalized.remove("adjustmentReason");
            }
            case MANUAL_PERCENTAGE -> {
                normalizePercentage(
                    normalized,
                    validateCurrentStep && currentStep >= 2
                );
                requireReasonWhenStrict(normalized, validateCurrentStep && currentStep >= 2);
                normalized.remove("professionalFixedAmount");
            }
            case FIXED_AMOUNT -> {
                normalizeFixedAmount(
                    normalized,
                    validateCurrentStep && currentStep >= 2
                );
                requireReasonWhenStrict(normalized, validateCurrentStep && currentStep >= 2);
                normalized.remove("professionalPercentagePercent");
            }
        }
        return normalized;
    }

    private void normalizeUuid(
        ObjectNode payload,
        String field,
        String label,
        boolean required
    ) {
        if (required) {
            payload.put(field, validator.requireUuid(payload, field, label).toString());
        } else if (payload.hasNonNull(field) && validator.optionalText(payload, field) != null) {
            payload.put(field, validator.optionalUuid(payload, field, label).toString());
        } else {
            payload.remove(field);
        }
    }

    private void normalizeDate(
        ObjectNode payload,
        String field,
        String label,
        boolean required
    ) {
        if (required) {
            payload.put(field, validator.requireDate(payload, field, label).toString());
        } else if (payload.hasNonNull(field) && validator.optionalText(payload, field) != null) {
            payload.put(field, validator.optionalDate(payload, field, label).toString());
        } else {
            payload.remove(field);
        }
    }

    private void normalizeMonth(
        ObjectNode payload,
        String field,
        String label,
        boolean required
    ) {
        if (required) {
            payload.put(field, validator.requireYearMonth(payload, field, label).toString());
        } else if (payload.hasNonNull(field) && validator.optionalText(payload, field) != null) {
            payload.put(field, validator.requireYearMonth(payload, field, label).toString());
        } else {
            payload.remove(field);
        }
    }

    private void normalizePayment(ObjectNode payload, boolean required) {
        String value = required
            ? validator.requireText(payload, "paymentStatus", "Situação do pagamento")
            : validator.optionalText(payload, "paymentStatus");
        if (value == null) {
            payload.remove("paymentStatus");
            normalizeDate(payload, "paidDate", "Data do pagamento", false);
            return;
        }

        PaymentStatus status;
        try {
            status = PaymentStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException("Situação do pagamento inválida.");
        }
        payload.put("paymentStatus", status.name());
        if (status == PaymentStatus.PENDING) {
            payload.remove("paidDate");
            return;
        }
        normalizeDate(payload, "paidDate", "Data do pagamento", required);
    }

    private void normalizePositiveDecimal(
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
            return;
        }
        if (value.signum() <= 0) {
            throw new DomainValidationException(label + " deve ser maior que zero.");
        }
        payload.put(field, value.toPlainString());
    }

    private void normalizePercentage(ObjectNode payload, boolean required) {
        BigDecimal value = required
            ? validator.requireDecimal(payload, "professionalPercentagePercent", "Percentual profissional")
            : validator.optionalDecimal(payload, "professionalPercentagePercent", "Percentual profissional");
        if (value == null) {
            payload.remove("professionalPercentagePercent");
            return;
        }
        if (value.signum() <= 0) {
            throw new DomainValidationException(
                "Para 0%, classifique o gasto como Pessoal."
            );
        }
        if (value.compareTo(new BigDecimal("100")) >= 0) {
            throw new DomainValidationException(
                "Para 100%, classifique o gasto como Profissional."
            );
        }
        payload.put("professionalPercentagePercent", value.toPlainString());
    }

    private void normalizeFixedAmount(ObjectNode payload, boolean required) {
        BigDecimal fixed = required
            ? validator.requireDecimal(payload, "professionalFixedAmount", "Valor profissional fixo")
            : validator.optionalDecimal(payload, "professionalFixedAmount", "Valor profissional fixo");
        if (fixed == null) {
            payload.remove("professionalFixedAmount");
            return;
        }
        BigDecimal total = validator.optionalDecimal(payload, "amount", "Valor");
        if (fixed.signum() <= 0) {
            throw new DomainValidationException(
                "Para nenhum valor profissional, classifique o gasto como Pessoal."
            );
        }
        if (total != null && fixed.compareTo(total) == 0) {
            throw new DomainValidationException(
                "Para atribuir todo o valor à operação, classifique o gasto como Profissional."
            );
        }
        if (total != null && fixed.compareTo(total) > 0) {
            throw new DomainValidationException(
                "O valor profissional deve ser menor que o valor total do gasto."
            );
        }
        payload.put("professionalFixedAmount", fixed.toPlainString());
    }

    private void requireReasonWhenStrict(ObjectNode payload, boolean required) {
        if (required) {
            validator.requireText(payload, "adjustmentReason", "Justificativa do ajuste");
        }
    }

    private ExpenseClassification optionalClassification(ObjectNode payload) {
        String value = validator.optionalText(payload, "classification");
        if (value == null) return null;
        try {
            return ExpenseClassification.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException("Classificação de gasto inválida.");
        }
    }

    private ExpenseClassification requireClassification(ObjectNode payload) {
        String value = validator.requireText(payload, "classification", "Classificação");
        try {
            return ExpenseClassification.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException("Classificação de gasto inválida.");
        }
    }

    private AllocationMethod optionalAllocationMethod(ObjectNode payload) {
        String value = validator.optionalText(payload, "allocationMethod");
        if (value == null) return null;
        try {
            return AllocationMethod.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException("Método de rateio inválido.");
        }
    }

    private enum PaymentStatus {
        PAID,
        PENDING
    }

    private AllocationMethod requireAllocationMethod(ObjectNode payload) {
        String value = validator.requireText(payload, "allocationMethod", "Método de rateio");
        try {
            return AllocationMethod.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException("Método de rateio inválido.");
        }
    }
}
