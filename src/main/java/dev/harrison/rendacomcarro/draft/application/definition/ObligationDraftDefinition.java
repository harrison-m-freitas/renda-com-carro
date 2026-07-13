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

    @Override
    public FormDraftType type() {
        return FormDraftType.OBLIGATION;
    }

    @Override
    public int schemaVersion() {
        return 1;
    }

    @Override
    public int maxStep() {
        return 4;
    }

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
    public ObjectNode normalizeAndValidate(ObjectNode payload, int currentStep) {
        validator.rejectUnknownFields(payload, ALLOWED_FIELDS);
        ObjectNode normalized = validator.sanitizeTextFields(payload, Set.of("creditor", "notes"));

        if (normalized.hasNonNull("vehicleId")) {
            UUID vehicleId = validator.optionalUuid(normalized, "vehicleId", "Veículo");
            if (vehicleId == null) {
                normalized.remove("vehicleId");
            } else {
                normalized.put("vehicleId", vehicleId.toString());
            }
        }

        if (currentStep >= 1) {
            validator.requireText(normalized, "creditor", "Credor");
            ObligationType type = requireType(normalized);
            normalized.put("type", type.name());
        }

        if (currentStep < 2) {
            return normalized;
        }

        ObligationMode mode = requireMode(normalized);
        normalized.put("mode", mode.name());
        BigDecimal principal = validator.requireDecimal(normalized, "principal", "Principal");
        if (principal.signum() <= 0) {
            throw new DomainValidationException("Principal deve ser maior que zero.");
        }
        normalized.put("principal", principal.toPlainString());
        LocalDate startDate = validator.requireDate(normalized, "startDate", "Data de início");
        normalized.put("startDate", startDate.toString());

        BigDecimal annualRate = validator.optionalDecimal(
            normalized, "annualRatePercent", "Juros anuais"
        );
        if (annualRate != null) {
            if (annualRate.signum() < 0 || annualRate.compareTo(new BigDecimal("100")) > 0) {
                throw new DomainValidationException("Juros anuais devem estar entre 0 e 100.");
            }
            normalized.put("annualRatePercent", annualRate.toPlainString());
        }

        if (currentStep < 3) {
            normalizeOptionalModeFields(normalized, mode, startDate);
            return normalized;
        }

        if (mode == ObligationMode.STRUCTURED) {
            LocalDate firstDueDate = validator.optionalDate(
                normalized, "firstDueDate", "Primeiro vencimento"
            );
            if (firstDueDate == null) {
                throw new DomainValidationException(
                    "O primeiro vencimento é obrigatório para obrigação estruturada."
                );
            }
            if (firstDueDate.isBefore(startDate)) {
                throw new DomainValidationException(
                    "O primeiro vencimento não pode ser anterior à data de início."
                );
            }
            Integer termMonths = validator.optionalInteger(
                normalized, "termMonths", "Parcelas"
            );
            if (termMonths == null || termMonths <= 0) {
                throw new DomainValidationException(
                    "A quantidade de parcelas deve ser maior que zero."
                );
            }
            normalized.put("firstDueDate", firstDueDate.toString());
            normalized.put("termMonths", termMonths);
            BigDecimal plannedInstallment = validator.optionalDecimal(
                normalized, "plannedInstallment", "Parcela prevista"
            );
            if (plannedInstallment != null) {
                if (plannedInstallment.signum() < 0) {
                    throw new DomainValidationException(
                        "Parcela prevista não pode ser negativa."
                    );
                }
                normalized.put("plannedInstallment", plannedInstallment.toPlainString());
            }
            normalized.remove("monthlyTarget");
        } else {
            BigDecimal monthlyTarget = validator.requireDecimal(
                normalized, "monthlyTarget", "Meta mensal flexível"
            );
            if (monthlyTarget.signum() <= 0) {
                throw new DomainValidationException(
                    "Meta mensal flexível deve ser maior que zero."
                );
            }
            normalized.put("monthlyTarget", monthlyTarget.toPlainString());
            normalized.remove("firstDueDate");
            normalized.remove("termMonths");
            normalized.remove("plannedInstallment");
        }
        return normalized;
    }

    private void normalizeOptionalModeFields(
        ObjectNode payload,
        ObligationMode mode,
        LocalDate startDate
    ) {
        if (mode == ObligationMode.STRUCTURED) {
            payload.remove("monthlyTarget");
            LocalDate firstDueDate = validator.optionalDate(
                payload, "firstDueDate", "Primeiro vencimento"
            );
            if (firstDueDate != null) {
                if (firstDueDate.isBefore(startDate)) {
                    throw new DomainValidationException(
                        "O primeiro vencimento não pode ser anterior à data de início."
                    );
                }
                payload.put("firstDueDate", firstDueDate.toString());
            }
            Integer termMonths = validator.optionalInteger(payload, "termMonths", "Parcelas");
            if (termMonths != null) {
                if (termMonths <= 0) {
                    throw new DomainValidationException(
                        "A quantidade de parcelas deve ser maior que zero."
                    );
                }
                payload.put("termMonths", termMonths);
            }
        } else {
            payload.remove("firstDueDate");
            payload.remove("termMonths");
            payload.remove("plannedInstallment");
            BigDecimal monthlyTarget = validator.optionalDecimal(
                payload, "monthlyTarget", "Meta mensal flexível"
            );
            if (monthlyTarget != null) {
                if (monthlyTarget.signum() <= 0) {
                    throw new DomainValidationException(
                        "Meta mensal flexível deve ser maior que zero."
                    );
                }
                payload.put("monthlyTarget", monthlyTarget.toPlainString());
            }
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

    private ObligationMode requireMode(ObjectNode payload) {
        String value = validator.requireText(payload, "mode", "Modo");
        try {
            return ObligationMode.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException("Modo da obrigação inválido.");
        }
    }
}
