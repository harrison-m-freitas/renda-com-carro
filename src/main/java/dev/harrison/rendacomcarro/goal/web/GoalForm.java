package dev.harrison.rendacomcarro.goal.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;

public class GoalForm {
    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth month = YearMonth.now();

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal personalNetGoal;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal operationalGoal;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal plannedHours;

    @NotBlank
    private String plannedDates;

    public String draftContextKey() {
        return month == null ? "" : "month:" + month;
    }

    public Set<LocalDate> parsedPlannedDates() {
        if (plannedDates == null || plannedDates.isBlank()) {
            throw new IllegalArgumentException("Informe pelo menos um dia planejado.");
        }
        try {
            TreeSet<LocalDate> dates = Arrays.stream(plannedDates.split("[,;\\s]+"))
                .filter(value -> !value.isBlank())
                .map(LocalDate::parse)
                .collect(Collectors.toCollection(TreeSet::new));
            if (dates.isEmpty()) {
                throw new IllegalArgumentException("Informe pelo menos um dia planejado.");
            }
            for (LocalDate date : dates) {
                if (month == null || !YearMonth.from(date).equals(month)) {
                    throw new IllegalArgumentException(
                        "Todos os dias planejados devem pertencer ao mês da meta."
                    );
                }
                if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    throw new IllegalArgumentException(
                        "Domingos não podem ser adicionados aos dias planejados."
                    );
                }
            }
            return Set.copyOf(dates);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Use datas no formato AAAA-MM-DD.", exception);
        }
    }

    public YearMonth getMonth() { return month; }
    public void setMonth(YearMonth month) { this.month = month; }
    public BigDecimal getPersonalNetGoal() { return personalNetGoal; }
    public void setPersonalNetGoal(BigDecimal personalNetGoal) { this.personalNetGoal = personalNetGoal; }
    public BigDecimal getOperationalGoal() { return operationalGoal; }
    public void setOperationalGoal(BigDecimal operationalGoal) { this.operationalGoal = operationalGoal; }
    public BigDecimal getPlannedHours() { return plannedHours; }
    public void setPlannedHours(BigDecimal plannedHours) { this.plannedHours = plannedHours; }
    public String getPlannedDates() { return plannedDates; }
    public void setPlannedDates(String plannedDates) { this.plannedDates = plannedDates; }
}
