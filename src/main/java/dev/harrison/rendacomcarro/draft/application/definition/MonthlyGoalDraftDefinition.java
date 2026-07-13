package dev.harrison.rendacomcarro.draft.application.definition;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.application.DraftPayloadValidator;
import dev.harrison.rendacomcarro.draft.application.FormDraftDefinition;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MonthlyGoalDraftDefinition implements FormDraftDefinition {
    private static final Pattern KEY_PATTERN = Pattern.compile("^month:(\\d{4}-\\d{2})$");
    private static final Set<String> ALLOWED_FIELDS = Set.of(
        "month", "personalNetGoal", "operationalGoal", "plannedHours", "plannedDates"
    );

    private final DraftPayloadValidator validator;

    public MonthlyGoalDraftDefinition(DraftPayloadValidator validator) {
        this.validator = validator;
    }

    @Override
    public FormDraftType type() {
        return FormDraftType.MONTHLY_GOAL;
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
        Matcher matcher = KEY_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            throw new DomainValidationException("A chave da meta deve identificar o mês.");
        }
        try {
            YearMonth.parse(matcher.group(1));
        } catch (DateTimeParseException exception) {
            throw new DomainValidationException("A chave da meta deve identificar um mês válido.");
        }
        return normalized;
    }

    @Override
    public ObjectNode normalizeAndValidate(ObjectNode payload, int currentStep) {
        validator.rejectUnknownFields(payload, ALLOWED_FIELDS);
        ObjectNode normalized = validator.sanitizeTextFields(payload, Set.of("plannedDates"));

        YearMonth month = null;
        if (currentStep >= 1 || normalized.hasNonNull("month")) {
            month = validator.requireYearMonth(normalized, "month", "Mês");
            normalized.put("month", month.toString());
        }

        if (currentStep >= 1) {
            putNonNegative(normalized, "personalNetGoal", "Meta líquida pessoal");
            putNonNegative(normalized, "operationalGoal", "Meta operacional");
        }

        if (normalized.hasNonNull("plannedHours")) {
            putNonNegative(normalized, "plannedHours", "Horas planejadas");
        }

        if (currentStep >= 2) {
            putNonNegative(normalized, "plannedHours", "Horas planejadas");
            String datesText = validator.requireText(
                normalized, "plannedDates", "Dias planejados"
            );
            if (month == null) {
                month = validator.requireYearMonth(normalized, "month", "Mês");
            }
            normalized.put("plannedDates", normalizeDates(datesText, month));
        } else if (normalized.hasNonNull("plannedDates")) {
            String datesText = validator.optionalText(normalized, "plannedDates");
            if (datesText == null) {
                normalized.remove("plannedDates");
            } else if (month != null) {
                normalized.put("plannedDates", normalizeDates(datesText, month));
            }
        }
        return normalized;
    }

    private void putNonNegative(ObjectNode payload, String field, String label) {
        BigDecimal value = validator.requireDecimal(payload, field, label);
        if (value.signum() < 0) {
            throw new DomainValidationException(label + " não pode ser negativo.");
        }
        payload.put(field, value.toPlainString());
    }

    private String normalizeDates(String raw, YearMonth month) {
        try {
            TreeSet<LocalDate> dates = Arrays.stream(raw.split("[,;\\s]+"))
                .filter(value -> !value.isBlank())
                .map(LocalDate::parse)
                .collect(Collectors.toCollection(TreeSet::new));
            if (dates.isEmpty()) {
                throw new DomainValidationException("Informe pelo menos um dia planejado.");
            }
            for (LocalDate date : dates) {
                if (!YearMonth.from(date).equals(month)) {
                    throw new DomainValidationException(
                        "Todos os dias planejados devem pertencer ao mês da meta."
                    );
                }
                if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    throw new DomainValidationException(
                        "Domingos não podem ser adicionados aos dias planejados."
                    );
                }
            }
            return dates.stream().map(LocalDate::toString).collect(Collectors.joining(","));
        } catch (DateTimeParseException exception) {
            throw new DomainValidationException("Use datas no formato AAAA-MM-DD.");
        }
    }
}
