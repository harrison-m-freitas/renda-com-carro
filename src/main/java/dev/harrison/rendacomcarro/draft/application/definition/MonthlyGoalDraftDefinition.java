package dev.harrison.rendacomcarro.draft.application.definition;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.application.DraftPayloadValidator;
import dev.harrison.rendacomcarro.draft.application.FormDraftDefinition;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.goal.domain.WorkloadPeriodicity;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
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
        "month",
        "vehicleIds",
        "personalNetGoal",
        "operationalGoal",
        "workloadPeriodicity",
        "workloadHours",
        "workloadMinutes",
        "plannedDates"
    );

    private final DraftPayloadValidator validator;

    public MonthlyGoalDraftDefinition(DraftPayloadValidator validator) {
        this.validator = validator;
    }

    @Override public FormDraftType type() { return FormDraftType.MONTHLY_GOAL; }
    @Override public int schemaVersion() { return 3; }
    @Override public int maxStep() { return 3; }

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
    public ObjectNode normalizeAndValidate(
        ObjectNode payload,
        int currentStep,
        boolean validateCurrentStep
    ) {
        validator.rejectUnknownFields(payload, ALLOWED_FIELDS);
        ObjectNode normalized = validator.sanitizeTextFields(
            payload,
            Set.of("plannedDates", "workloadPeriodicity")
        );

        YearMonth month = normalizeMonth(normalized, currentStep, validateCurrentStep);
        normalizeVehicleIds(normalized, validateCurrentStep && currentStep >= 1);
        normalizeNonNegativeDecimal(
            normalized,
            "personalNetGoal",
            "Meta líquida pessoal",
            validateCurrentStep && currentStep >= 1
        );
        normalizeNonNegativeDecimal(
            normalized,
            "operationalGoal",
            "Meta operacional",
            validateCurrentStep && currentStep >= 1
        );

        boolean workloadRequired = validateCurrentStep && currentStep >= 2;
        normalizePeriodicity(normalized, workloadRequired);
        Integer hours = normalizeInteger(
            normalized,
            "workloadHours",
            "Horas da jornada",
            workloadRequired,
            0,
            Integer.MAX_VALUE
        );
        Integer minutes = normalizeInteger(
            normalized,
            "workloadMinutes",
            "Os minutos",
            workloadRequired,
            0,
            59
        );
        if (workloadRequired && ((long) hours * 60 + minutes) <= 0) {
            throw new DomainValidationException("A duração informada deve ser maior que zero.");
        }

        String datesText = validator.optionalText(normalized, "plannedDates");
        if (datesText == null && workloadRequired) {
            datesText = validator.requireText(normalized, "plannedDates", "Dias planejados");
        }
        if (datesText == null) {
            normalized.remove("plannedDates");
        } else if (month != null) {
            normalized.put("plannedDates", normalizeDates(datesText, month));
        } else {
            normalized.put("plannedDates", datesText);
        }
        return normalized;
    }

    private void normalizeVehicleIds(ObjectNode payload, boolean required) {
        if (!payload.has("vehicleIds")) {
            if (required) {
                validator.requireUuidArray(payload, "vehicleIds", "Veículos");
            }
            return;
        }
        if (payload.path("vehicleIds").isArray() && payload.path("vehicleIds").isEmpty()) {
            if (required) {
                validator.requireUuidArray(payload, "vehicleIds", "Veículos");
            }
            return;
        }
        Set<java.util.UUID> values = validator.requireUuidArray(
            payload,
            "vehicleIds",
            "Veículos"
        );
        ArrayNode normalized = payload.putArray("vehicleIds");
        values.stream().sorted().map(java.util.UUID::toString).forEach(normalized::add);
    }

    private YearMonth normalizeMonth(
        ObjectNode payload,
        int currentStep,
        boolean validateCurrentStep
    ) {
        String monthText = validator.optionalText(payload, "month");
        if (monthText != null) {
            YearMonth month = validator.requireYearMonth(payload, "month", "Mês");
            payload.put("month", month.toString());
            return month;
        }
        if (validateCurrentStep && currentStep >= 1) {
            return validator.requireYearMonth(payload, "month", "Mês");
        }
        payload.remove("month");
        return null;
    }

    private void normalizePeriodicity(ObjectNode payload, boolean required) {
        String raw = validator.optionalText(payload, "workloadPeriodicity");
        if (raw == null) {
            if (required) {
                throw new DomainValidationException("Periodicidade da jornada é obrigatória.");
            }
            payload.remove("workloadPeriodicity");
            return;
        }
        try {
            WorkloadPeriodicity periodicity = WorkloadPeriodicity.valueOf(
                raw.toUpperCase(Locale.ROOT)
            );
            payload.put("workloadPeriodicity", periodicity.name());
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException("Periodicidade da jornada é inválida.");
        }
    }

    private Integer normalizeInteger(
        ObjectNode payload,
        String field,
        String label,
        boolean required,
        int minimum,
        int maximum
    ) {
        Integer value = validator.optionalInteger(payload, field, label);
        if (value == null) {
            if (required) {
                throw new DomainValidationException(label + " é obrigatório.");
            }
            payload.remove(field);
            return null;
        }
        if (value < minimum || value > maximum) {
            if (field.equals("workloadMinutes")) {
                throw new DomainValidationException("Os minutos devem estar entre 0 e 59.");
            }
            throw new DomainValidationException(label + " deve ser um número não negativo.");
        }
        payload.put(field, value);
        return value;
    }

    private void normalizeNonNegativeDecimal(
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
