package dev.harrison.rendacomcarro.goal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "planned_work_day")
public class PlannedWorkDay {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monthly_goal_id")
    private MonthlyGoal monthlyGoal;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "planned_hours", nullable = false, precision = 6, scale = 2)
    private BigDecimal plannedHours;

    @Column(nullable = false)
    private boolean available;

    protected PlannedWorkDay() {}

    public static PlannedWorkDay create(
        MonthlyGoal goal,
        LocalDate date,
        long allocatedMinutes
    ) {
        if (goal == null || date == null || allocatedMinutes < 0) {
            throw new IllegalArgumentException("Dia planejado inválido");
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Domingo não pode ser planejado no MVP");
        }

        PlannedWorkDay day = new PlannedWorkDay();
        day.id = UUID.randomUUID();
        day.monthlyGoal = goal;
        day.workDate = date;
        day.plannedHours = BigDecimal.valueOf(allocatedMinutes)
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        day.available = true;
        return day;
    }

    public static PlannedWorkDay create(
        MonthlyGoal goal,
        LocalDate date,
        BigDecimal hours
    ) {
        if (hours == null || hours.signum() < 0) {
            throw new IllegalArgumentException("Dia planejado inválido");
        }
        long minutes = hours.multiply(BigDecimal.valueOf(60))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
        return create(goal, date, minutes);
    }

    public UUID getId() { return id; }
    public LocalDate getWorkDate() { return workDate; }
    public BigDecimal getPlannedHours() { return plannedHours; }
    public boolean isAvailable() { return available; }
}
