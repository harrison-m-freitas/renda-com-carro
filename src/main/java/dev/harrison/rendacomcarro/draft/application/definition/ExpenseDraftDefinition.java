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
import org.springframework.stereotype.Component;

@Component
public class ExpenseDraftDefinition implements FormDraftDefinition {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
        "vehicleId", "categoryId", "expenseDate", "competenceMonth", "paidDate",
        "amount", "classification", "allocationMethod",
        "professionalPercentagePercent", "professionalFixedAmount",
        "adjustmentReason", "notes"
    );
    private static final Set<String> TEXT_FIELDS = Set.of("adjustmentReason", "notes");

    private final DraftPayloadValidator validator;

    public ExpenseDraftDefinition(DraftPayloadValidator validator) {
        this.validator = validator;
    }

    @Override
    public FormDraftType type() {
        return FormDraftType.EXPENSE;
    }

    @Override
    public int schemaVersion() {
        return 1;
    }

    @Override
    public int maxStep() {
        return 3;
    }

    @Override
    public String normalizeContextKey(String contextKey) {
        String normalized = contextKey == null ? "" : contextKey.trim();
        if (!"current".equals(normalized)) {
            throw new DomainValidationException("A chave do rascunho de gasto deve ser current.");
        }
        return normalized;
    }

    @Override
    public ObjectNode normalizeAndValidate(ObjectNode payload, int currentStep) {
        validator.rejectUnknownFields(payload, ALLOWED_FIELDS);
        ObjectNode normalized = validator.sanitizeTextFields(payload, TEXT_FIELDS);

        if (currentStep >= 1) {
            normalized.put("vehicleId", validator.requireUuid(
                normalized, "vehicleId", "Veículo"
            ).toString());
            normalized.put("categoryId", validator.requireUuid(
                normalized, "categoryId", "Categoria"
            ).toString());
            normalized.put("expenseDate", validator.requireDate(
                normalized, "expenseDate", "Data do gasto"
            ).toString());
            BigDecimal amount = validator.requireDecimal(normalized, "amount", "Valor");
            if (amount.signum() <= 0) {
                throw new DomainValidationException("Valor deve ser maior que zero.");
            }
            normalized.put("amount", amount.toPlainString());
        }

        if (normalized.hasNonNull("paidDate")) {
            var paidDate = validator.optionalDate(normalized, "paidDate", "Data do pagamento");
            if (paidDate == null) {
                normalized.remove("paidDate");
            } else {
                normalized.put("paidDate", paidDate.toString());
            }
        }

        if (currentStep < 2) {
            return normalized;
        }

        normalized.put("competenceMonth", validator.requireYearMonth(
            normalized, "competenceMonth", "Competência"
        ).toString());

        ExpenseClassification classification = requireClassification(normalized);
        normalized.put("classification", classification.name());

        if (classification != ExpenseClassification.MIXED) {
            normalized.remove("allocationMethod");
            normalized.remove("professionalPercentagePercent");
            normalized.remove("professionalFixedAmount");
            normalized.remove("adjustmentReason");
            return normalized;
        }

        AllocationMethod method = requireAllocationMethod(normalized);
        normalized.put("allocationMethod", method.name());
        switch (method) {
            case MILEAGE_RATIO -> {
                normalized.remove("professionalPercentagePercent");
                normalized.remove("professionalFixedAmount");
                normalized.remove("adjustmentReason");
            }
            case MANUAL_PERCENTAGE -> {
                BigDecimal percentage = validator.requireDecimal(
                    normalized, "professionalPercentagePercent", "Percentual profissional"
                );
                if (percentage.signum() < 0 || percentage.compareTo(new BigDecimal("100")) > 0) {
                    throw new DomainValidationException(
                        "Percentual profissional deve estar entre 0 e 100."
                    );
                }
                normalized.put("professionalPercentagePercent", percentage.toPlainString());
                validator.requireText(normalized, "adjustmentReason", "Justificativa do ajuste");
                normalized.remove("professionalFixedAmount");
            }
            case FIXED_AMOUNT -> {
                BigDecimal fixedAmount = validator.requireDecimal(
                    normalized, "professionalFixedAmount", "Valor profissional fixo"
                );
                BigDecimal total = validator.requireDecimal(normalized, "amount", "Valor");
                if (fixedAmount.signum() < 0 || fixedAmount.compareTo(total) > 0) {
                    throw new DomainValidationException(
                        "Valor profissional fixo deve estar entre zero e o valor do gasto."
                    );
                }
                normalized.put("professionalFixedAmount", fixedAmount.toPlainString());
                validator.requireText(normalized, "adjustmentReason", "Justificativa do ajuste");
                normalized.remove("professionalPercentagePercent");
            }
        }
        return normalized;
    }

    private ExpenseClassification requireClassification(ObjectNode payload) {
        String value = validator.requireText(payload, "classification", "Classificação");
        try {
            return ExpenseClassification.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException("Classificação de gasto inválida.");
        }
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
