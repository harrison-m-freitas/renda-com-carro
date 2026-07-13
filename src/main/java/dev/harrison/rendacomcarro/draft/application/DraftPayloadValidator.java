package dev.harrison.rendacomcarro.draft.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import dev.harrison.rendacomcarro.shared.domain.FlexibleDecimalParser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DraftPayloadValidator {
    public void rejectUnknownFields(ObjectNode payload, Set<String> allowedFields) {
        if (payload == null) {
            throw new DomainValidationException("O conteúdo do rascunho é obrigatório.");
        }
        Set<String> unknown = new HashSet<>();
        Iterator<String> names = payload.fieldNames();
        while (names.hasNext()) {
            String field = names.next();
            if (!allowedFields.contains(field)) {
                unknown.add(field);
            }
        }
        if (!unknown.isEmpty()) {
            throw new DomainValidationException(
                "Campo de rascunho não permitido: " + unknown.stream().sorted().findFirst().orElseThrow()
            );
        }
    }

    public String requireText(ObjectNode payload, String field, String label) {
        String value = optionalText(payload, field);
        if (value == null) {
            throw new DomainValidationException(label + " é obrigatório.");
        }
        return value;
    }

    public String optionalText(ObjectNode payload, String field) {
        JsonNode node = payload.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw new DomainValidationException("O campo " + field + " deve ser textual.");
        }
        String value = sanitize(node.textValue()).trim();
        return value.isBlank() ? null : value;
    }

    public UUID requireUuid(ObjectNode payload, String field, String label) {
        String value = requireText(payload, field, label);
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException(label + " é inválido.");
        }
    }

    public UUID optionalUuid(ObjectNode payload, String field, String label) {
        String value = optionalText(payload, field);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException(label + " é inválido.");
        }
    }

    public LocalDate requireDate(ObjectNode payload, String field, String label) {
        String value = requireText(payload, field, label);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new DomainValidationException(label + " deve usar o formato AAAA-MM-DD.");
        }
    }

    public LocalDate optionalDate(ObjectNode payload, String field, String label) {
        String value = optionalText(payload, field);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new DomainValidationException(label + " deve usar o formato AAAA-MM-DD.");
        }
    }

    public YearMonth requireYearMonth(ObjectNode payload, String field, String label) {
        String value = requireText(payload, field, label);
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException exception) {
            throw new DomainValidationException(label + " deve usar o formato AAAA-MM.");
        }
    }

    public BigDecimal requireDecimal(ObjectNode payload, String field, String label) {
        String value = requireText(payload, field, label);
        return parseDecimal(value, label);
    }

    public BigDecimal optionalDecimal(ObjectNode payload, String field, String label) {
        String value = optionalText(payload, field);
        return value == null ? null : parseDecimal(value, label);
    }

    public Integer optionalInteger(ObjectNode payload, String field, String label) {
        JsonNode node = payload.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value;
        if (node.isIntegralNumber()) {
            return node.intValue();
        }
        if (!node.isTextual()) {
            throw new DomainValidationException(label + " deve ser um número inteiro.");
        }
        value = node.textValue().trim();
        if (value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new DomainValidationException(label + " deve ser um número inteiro.");
        }
    }

    public boolean booleanValue(ObjectNode payload, String field) {
        JsonNode node = payload.get(field);
        if (node == null || node.isNull()) {
            return false;
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isTextual()) {
            String value = node.textValue().trim();
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
            if ("false".equalsIgnoreCase(value) || value.isBlank()) {
                return false;
            }
        }
        throw new DomainValidationException("O campo " + field + " deve ser verdadeiro ou falso.");
    }

    public ObjectNode sanitizeTextFields(ObjectNode payload, Set<String> textFields) {
        ObjectNode normalized = payload.deepCopy();
        for (String field : textFields) {
            JsonNode node = normalized.get(field);
            if (node != null && node.isTextual()) {
                normalized.put(field, sanitize(node.textValue()));
            }
        }
        return normalized;
    }

    public String sanitize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\n' || character == '\r' || character == '\t'
                || !Character.isISOControl(character)) {
                result.append(character);
            }
        }
        return result.toString();
    }

    private BigDecimal parseDecimal(String value, String label) {
        try {
            return FlexibleDecimalParser.parse(value);
        } catch (NumberFormatException exception) {
            throw new DomainValidationException(label + " deve ser um número válido.");
        }
    }
}
