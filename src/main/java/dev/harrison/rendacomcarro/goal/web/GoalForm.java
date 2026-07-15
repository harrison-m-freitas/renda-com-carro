package dev.harrison.rendacomcarro.goal.web;

import dev.harrison.rendacomcarro.goal.domain.WorkloadPeriodicity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;

public class GoalForm {
    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth month = YearMonth.now();

    @NotEmpty(message = "Selecione pelo menos um veículo")
    private Set<UUID> vehicleIds = new LinkedHashSet<>();

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal personalNetGoal;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal operationalGoal;

    @NotNull
    private WorkloadPeriodicity workloadPeriodicity = WorkloadPeriodicity.MONTHLY;

    @NotNull
    @Min(0)
    private Long workloadHours;

    @NotNull
    @Min(0)
    @Max(59)
    private Integer workloadMinutes = 0;

    @NotBlank
    private String plannedDates;

    public String draftContextKey() {
        return month == null ? "" : "month:" + month;
    }

    public long enteredDurationMinutes() {
        if (workloadPeriodicity == null) {
            throw new IllegalArgumentException("Selecione como a jornada será informada.");
        }
        if (workloadHours == null || workloadHours < 0) {
            throw new IllegalArgumentException("Informe uma quantidade válida de horas.");
        }
        if (workloadMinutes == null || workloadMinutes < 0 || workloadMinutes > 59) {
            throw new IllegalArgumentException("Os minutos devem estar entre 0 e 59.");
        }
        long total = Math.addExact(Math.multiplyExact(workloadHours, 60), workloadMinutes);
        if (total <= 0) {
            throw new IllegalArgumentException("A duração informada deve ser maior que zero.");
        }
        return total;
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
    public Set<UUID> getVehicleIds() { return Set.copyOf(vehicleIds); }
    public void setVehicleIds(Set<UUID> vehicleIds) {
        this.vehicleIds = vehicleIds == null
            ? new LinkedHashSet<>()
            : new LinkedHashSet<>(vehicleIds);
    }
    public BigDecimal getPersonalNetGoal() { return personalNetGoal; }
    public void setPersonalNetGoal(BigDecimal personalNetGoal) { this.personalNetGoal = personalNetGoal; }
    public BigDecimal getOperationalGoal() { return operationalGoal; }
    public void setOperationalGoal(BigDecimal operationalGoal) { this.operationalGoal = operationalGoal; }
    public WorkloadPeriodicity getWorkloadPeriodicity() { return workloadPeriodicity; }
    public void setWorkloadPeriodicity(WorkloadPeriodicity workloadPeriodicity) {
        this.workloadPeriodicity = workloadPeriodicity;
    }
    public Long getWorkloadHours() { return workloadHours; }
    public void setWorkloadHours(Long workloadHours) { this.workloadHours = workloadHours; }
    public Integer getWorkloadMinutes() { return workloadMinutes; }
    public void setWorkloadMinutes(Integer workloadMinutes) { this.workloadMinutes = workloadMinutes; }
    public String getPlannedDates() { return plannedDates; }
    public void setPlannedDates(String plannedDates) { this.plannedDates = plannedDates; }
}
