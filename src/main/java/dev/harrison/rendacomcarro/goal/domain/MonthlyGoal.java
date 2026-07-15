package dev.harrison.rendacomcarro.goal.domain;

import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@Table(name = "monthly_goal")
public class MonthlyGoal {
    @Id
    private UUID id;

    @Column(name = "reference_month", nullable = false, unique = true)
    private LocalDate referenceMonth;

    @Column(name = "personal_net_goal", nullable = false, precision = 14, scale = 2)
    private BigDecimal personalNetGoal;

    @Column(name = "operational_goal", nullable = false, precision = 14, scale = 2)
    private BigDecimal operationalGoal;

    @Column(name = "planned_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal plannedHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "workload_periodicity", nullable = false, length = 16)
    private WorkloadPeriodicity workloadPeriodicity;

    @Column(name = "entered_duration_minutes", nullable = false)
    private long enteredDurationMinutes;

    @Column(name = "calculated_month_minutes", nullable = false)
    private long calculatedMonthMinutes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected MonthlyGoal() {}

    public static MonthlyGoal create(
        YearMonth month,
        BigDecimal personal,
        BigDecimal operational,
        WorkloadPeriodicity periodicity,
        long enteredMinutes,
        long calculatedMinutes
    ) {
        validate(month, personal, operational, periodicity, enteredMinutes, calculatedMinutes);

        MonthlyGoal goal = new MonthlyGoal();
        goal.id = UUID.randomUUID();
        goal.apply(month, personal, operational, periodicity, enteredMinutes, calculatedMinutes);
        goal.createdAt = LocalDateTime.now();
        goal.updatedAt = goal.createdAt;
        return goal;
    }

    public static MonthlyGoal create(
        YearMonth month,
        BigDecimal personal,
        BigDecimal operational,
        BigDecimal hours
    ) {
        if (hours == null || hours.signum() < 0) {
            throw new IllegalArgumentException("Meta mensal inválida");
        }
        long minutes = hours.multiply(BigDecimal.valueOf(60))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
        return create(
            month,
            personal,
            operational,
            WorkloadPeriodicity.MONTHLY,
            minutes,
            minutes
        );
    }

    public void update(
        YearMonth month,
        BigDecimal personal,
        BigDecimal operational,
        WorkloadPeriodicity periodicity,
        long enteredMinutes,
        long calculatedMinutes
    ) {
        validate(month, personal, operational, periodicity, enteredMinutes, calculatedMinutes);
        apply(month, personal, operational, periodicity, enteredMinutes, calculatedMinutes);
        updatedAt = LocalDateTime.now();
    }

    public void assignVehicle(Vehicle selectedVehicle) {
        if (selectedVehicle == null) {
            throw new IllegalArgumentException("Veículo da meta é obrigatório.");
        }
        if (vehicle != null && !vehicle.getId().equals(selectedVehicle.getId())) {
            throw new IllegalStateException("O veículo de uma meta existente não pode ser alterado.");
        }
        vehicle = selectedVehicle;
        updatedAt = LocalDateTime.now();
    }

    private void apply(
        YearMonth month,
        BigDecimal personal,
        BigDecimal operational,
        WorkloadPeriodicity periodicity,
        long enteredMinutes,
        long calculatedMinutes
    ) {
        referenceMonth = month.atDay(1);
        personalNetGoal = DecimalPolicy.money(personal);
        operationalGoal = DecimalPolicy.money(operational);
        workloadPeriodicity = periodicity;
        enteredDurationMinutes = enteredMinutes;
        calculatedMonthMinutes = calculatedMinutes;
        plannedHours = hoursFromMinutes(calculatedMinutes);
    }

    private static void validate(
        YearMonth month,
        BigDecimal personal,
        BigDecimal operational,
        WorkloadPeriodicity periodicity,
        long enteredMinutes,
        long calculatedMinutes
    ) {
        if (month == null || personal == null || operational == null || periodicity == null
            || personal.signum() < 0 || operational.signum() < 0
            || enteredMinutes < 0 || calculatedMinutes < 0) {
            throw new IllegalArgumentException("Meta mensal inválida");
        }
    }

    private static BigDecimal hoursFromMinutes(long minutes) {
        return BigDecimal.valueOf(minutes)
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public UUID getId() { return id; }
    public YearMonth getMonth() { return YearMonth.from(referenceMonth); }
    public LocalDate getReferenceMonth() { return referenceMonth; }
    public BigDecimal getPersonalNetGoal() { return personalNetGoal; }
    public BigDecimal getOperationalGoal() { return operationalGoal; }
    public BigDecimal getPlannedHours() { return plannedHours; }
    public WorkloadPeriodicity getWorkloadPeriodicity() { return workloadPeriodicity; }
    public long getEnteredDurationMinutes() { return enteredDurationMinutes; }
    public long getCalculatedMonthMinutes() { return calculatedMonthMinutes; }
    public long getEnteredHours() { return enteredDurationMinutes / 60; }
    public int getEnteredRemainderMinutes() { return (int) (enteredDurationMinutes % 60); }
    public long getCalculatedHours() { return calculatedMonthMinutes / 60; }
    public int getCalculatedRemainderMinutes() { return (int) (calculatedMonthMinutes % 60); }
    public Vehicle getVehicle() { return vehicle; }
}
