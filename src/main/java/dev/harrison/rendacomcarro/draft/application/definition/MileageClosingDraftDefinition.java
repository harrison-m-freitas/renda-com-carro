package dev.harrison.rendacomcarro.draft.application.definition;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.application.DraftPayloadValidator;
import dev.harrison.rendacomcarro.draft.application.FormDraftDefinition;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MileageClosingDraftDefinition implements FormDraftDefinition {
    private static final Pattern KEY_PATTERN = Pattern.compile(
        "^vehicle:([0-9a-fA-F-]{36}):month:(\\d{4}-\\d{2})$"
    );
    private static final Set<String> ALLOWED_FIELDS = Set.of(
        "manualAdjustment", "initialOdometer", "finalOdometer",
        "professionalKilometers", "adjustmentReason", "confirmWarnings"
    );

    private final DraftPayloadValidator validator;

    public MileageClosingDraftDefinition(DraftPayloadValidator validator) {
        this.validator = validator;
    }

    @Override public FormDraftType type() { return FormDraftType.MILEAGE_CLOSING; }
    @Override public int schemaVersion() { return 1; }
    @Override public int maxStep() { return 3; }

    @Override
    public String normalizeContextKey(String contextKey) {
        String normalized = contextKey == null ? "" : contextKey.trim();
        Matcher matcher = KEY_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            throw new DomainValidationException(
                "A chave do fechamento deve identificar veículo e mês."
            );
        }
        try {
            UUID.fromString(matcher.group(1));
            YearMonth.parse(matcher.group(2));
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new DomainValidationException(
                "A chave do fechamento deve identificar veículo e mês válidos."
            );
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
        ObjectNode normalized = validator.sanitizeTextFields(payload, Set.of("adjustmentReason"));
        boolean manualAdjustment = validator.booleanValue(normalized, "manualAdjustment");
        normalized.put("manualAdjustment", manualAdjustment);
        normalized.put("confirmWarnings", validator.booleanValue(normalized, "confirmWarnings"));

        if (!manualAdjustment) {
            normalized.remove("initialOdometer");
            normalized.remove("finalOdometer");
            normalized.remove("professionalKilometers");
            normalized.remove("adjustmentReason");
            return normalized;
        }

        validateOptionalNonNegative(normalized, "initialOdometer", "Odômetro inicial");
        validateOptionalNonNegative(normalized, "finalOdometer", "Odômetro final");
        validateOptionalNonNegative(
            normalized, "professionalKilometers", "Quilômetros profissionais"
        );

        if (validateCurrentStep && currentStep >= 3) {
            BigDecimal initial = requireNonNegative(
                normalized, "initialOdometer", "Odômetro inicial"
            );
            BigDecimal end = requireNonNegative(
                normalized, "finalOdometer", "Odômetro final"
            );
            BigDecimal professional = requireNonNegative(
                normalized, "professionalKilometers", "Quilômetros profissionais"
            );
            if (end.compareTo(initial) < 0) {
                throw new DomainValidationException(
                    "Odômetro final não pode ser menor que o inicial."
                );
            }
            if (professional.compareTo(end.subtract(initial)) > 0) {
                throw new DomainValidationException(
                    "Quilômetros profissionais não podem exceder o total rodado."
                );
            }
            if (validator.optionalText(normalized, "adjustmentReason") == null) {
                throw new DomainValidationException(
                    "A justificativa da correção é obrigatória."
                );
            }
        }
        return normalized;
    }

    private void validateOptionalNonNegative(ObjectNode payload, String field, String label) {
        BigDecimal value = validator.optionalDecimal(payload, field, label);
        if (value == null) {
            payload.remove(field);
            return;
        }
        if (value.signum() < 0) {
            throw new DomainValidationException(label + " não pode ser negativo.");
        }
        payload.put(field, value.toPlainString());
    }

    private BigDecimal requireNonNegative(ObjectNode payload, String field, String label) {
        BigDecimal value = validator.requireDecimal(payload, field, label);
        if (value.signum() < 0) {
            throw new DomainValidationException(label + " não pode ser negativo.");
        }
        payload.put(field, value.toPlainString());
        return value;
    }
}
