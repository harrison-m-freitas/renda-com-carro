package dev.harrison.rendacomcarro.draft.application.definition;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.application.DraftPayloadValidator;
import dev.harrison.rendacomcarro.draft.application.FormDraftDefinition;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
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
    private static final Pattern KEY_PATTERN = Pattern.compile(
        "^draft:([0-9a-fA-F-]{36})$"
    );
    private static final Set<String> ALLOWED_FIELDS = Set.of(
        "vehicleId", "creditor", "type", "mode", "principal",
        "annualRatePercent", "startDate", "firstDueDate", "termMonths",
        "plannedInstallment", "monthlyTarget", "notes"
    );

    private final DraftPayloadValidator validator;

    public ObligationDraftDefinition(DraftPayloadValidator validator) {
        this.validator = validator;
    }

    @Override public FormDraftType type() { return FormDraftType.OBLIGATION; }
    @Override public int schemaVersion() { return 1; }
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
        ObjectNode normalized = validator.sanitizeTextFields(payload, Set.of("creditor", "notes"));

        normalizeVehicle(normalized);

        String creditor = validator.optionalText(normalized, "creditor");
        if (creditor == null && validateCurrentStep && currentStep >= 1) {
            validator.requireText(normalized, "creditor", "Credor");
        } else if (creditor == null) {
            normalized.remove("creditor");
        }

        ObligationType type = optionalType(normalized);
        if (type == null && validateCurrentStep && currentStep >= 1) {
            type = requireType(normalized);
        }
        if (type != null) normalized.put("type", type.name());

        ObligationMode mode = optionalMode(normalized);
        if (mode == null && validateCurrentStep && currentStep >= 2) {
            mode = requireMode(normalized);
        }
        if (mode != null) normalized.put("mode", mode.name());

        normalizePrincipal(normalized, validateCurrentStep && currentStep >= 2);
        LocalDate startDate = normalizeStartDate(
            normalized,
            validateCurrentStep && currentStep >= 2
        );
        normalizeAnnualRate(normalized);

        if (mode == null) {
            return normalized;
        }

        if (mode == ObligationMode.STRUCTURED) {
            normalized.remove("monthlyTarget");
            normalizeStructuredFields(
                normalized,
                startDate,
                validateCurrentStep && currentStep >= 3
            );
        } else {
            normalized.remove("firstDueDate");
            normalized.remove("termMonths");
            normalized.remove("plannedInstallment");
            normalizeMonthlyTarget(
                normalized,
                validateCurrentStep && currentStep >= 3
            );
        }
        return normalized;
    }

    private void normalizeVehicle(ObjectNode payload) {
        String raw = validator.optionalText(payload, "vehicleId");
        if (raw == null) {
            payload.remove("vehicleId");
            return;
        }
        payload.put("vehicleId", validator.optionalUuid(payload, "vehicleId", "Veículo").toString());
    }

    private void normalizePrincipal(ObjectNode payload, boolean required) {
        BigDecimal principal = required
            ? validator.requireDecimal(payload, "principal", "Principal")
            : validator.optionalDecimal(payload, "principal", "Principal");
        if (principal == null) {
            payload.remove("principal");
            return;
        }
        if (principal.signum() <= 0) {
            throw new DomainValidationException("Principal deve ser maior que zero.");
        }
        payload.put("principal", principal.toPlainString());
    }

    private LocalDate normalizeStartDate(ObjectNode payload, boolean required) {
        String raw = validator.optionalText(payload, "startDate");
        if (raw == null && required) {
            LocalDate date = validator.requireDate(payload, "startDate", "Data de início");
            payload.put("startDate", date.toString());
            return date;
        }
        if (raw == null) {
            payload.remove("startDate");
            return null;
        }
        LocalDate date = validator.optionalDate(payload, "startDate", "Data de início");
        payload.put("startDate", date.toString());
        return date;
    }

    private void normalizeAnnualRate(ObjectNode payload) {
        BigDecimal annualRate = validator.optionalDecimal(
            payload, "annualRatePercent", "Juros anuais"
        );
        if (annualRate == null) {
            payload.remove("annualRatePercent");
            return;
        }
        if (annualRate.signum() < 0 || annualRate.compareTo(new BigDecimal("100")) > 0) {
            throw new DomainValidationException("Juros anuais devem estar entre 0 e 100.");
        }
        payload.put("annualRatePercent", annualRate.toPlainString());
    }

    private void normalizeStructuredFields(
        ObjectNode payload,
        LocalDate startDate,
        boolean required
    ) {
        LocalDate firstDueDate = validator.optionalDate(
            payload, "firstDueDate", "Primeiro vencimento"
        );
        if (firstDueDate == null && required) {
            throw new DomainValidationException(
                "O primeiro vencimento é obrigatório para obrigação estruturada."
            );
        }
        if (firstDueDate == null) {
            payload.remove("firstDueDate");
        } else {
            if (startDate != null && firstDueDate.isBefore(startDate)) {
                throw new DomainValidationException(
                    "O primeiro vencimento não pode ser anterior à data de início."
                );
            }
            payload.put("firstDueDate", firstDueDate.toString());
        }

        Integer termMonths = validator.optionalInteger(payload, "termMonths", "Parcelas");
        if (termMonths == null && required) {
            throw new DomainValidationException(
                "A quantidade de parcelas deve ser maior que zero."
            );
        }
        if (termMonths == null) {
            payload.remove("termMonths");
        } else {
            if (termMonths <= 0) {
                throw new DomainValidationException(
                    "A quantidade de parcelas deve ser maior que zero."
                );
            }
            payload.put("termMonths", termMonths);
        }

        BigDecimal planned = validator.optionalDecimal(
            payload, "plannedInstallment", "Parcela prevista"
        );
        if (planned == null) {
            payload.remove("plannedInstallment");
        } else {
            if (planned.signum() < 0) {
                throw new DomainValidationException("Parcela prevista não pode ser negativa.");
            }
            payload.put("plannedInstallment", planned.toPlainString());
        }
    }

    private void normalizeMonthlyTarget(ObjectNode payload, boolean required) {
        BigDecimal target = required
            ? validator.requireDecimal(payload, "monthlyTarget", "Meta mensal flexível")
            : validator.optionalDecimal(payload, "monthlyTarget", "Meta mensal flexível");
        if (target == null) {
            payload.remove("monthlyTarget");
            return;
        }
        if (target.signum() <= 0) {
            throw new DomainValidationException(
                "Meta mensal flexível deve ser maior que zero."
            );
        }
        payload.put("monthlyTarget", target.toPlainString());
    }

    private ObligationType optionalType(ObjectNode payload) {
        String value = validator.optionalText(payload, "type");
        if (value == null) return null;
        try {
            return ObligationType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException("Tipo de obrigação inválido.");
        }
    }

    private ObligationType requireType(ObjectNode payload) {
        String value = validator.requireText(payload, "type", "Tipo");
        try {
            return ObligationType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException("Tipo de obrigação inválido.");
        }
    }

    private ObligationMode optionalMode(ObjectNode payload) {
        String value = validator.optionalText(payload, "mode");
        if (value == null) return null;
        try {
            return ObligationMode.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException("Modo da obrigação inválido.");
        }
    }

    private ObligationMode requireMode(ObjectNode payload) {
        String value = validator.requireText(payload, "mode", "Modo");
        try {
            return ObligationMode.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException("Modo da obrigação inválido.");
        }
    }
}
