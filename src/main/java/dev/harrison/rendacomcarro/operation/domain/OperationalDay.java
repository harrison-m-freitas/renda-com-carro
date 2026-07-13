package dev.harrison.rendacomcarro.operation.domain;

import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import dev.harrison.rendacomcarro.shared.domain.DomainConflictException;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "operational_day")
public class OperationalDay {
    @Id private UUID id;
    @Column(name = "operation_date", nullable = false) private LocalDate date;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "vehicle_id") private Vehicle vehicle;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private OperationalDayStatus status;
    @Column(name = "planned_goal", nullable = false, precision = 14, scale = 2) private BigDecimal plannedGoal;
    @Column(name = "initial_odometer", nullable = false, precision = 12, scale = 1) private BigDecimal initialOdometer;
    @Column(name = "final_odometer", precision = 12, scale = 1) private BigDecimal finalOdometer;
    @Column(name = "opened_at", nullable = false) private LocalDateTime openedAt;
    @Column(name = "closed_at") private LocalDateTime closedAt;
    @Column(columnDefinition = "text") private String notes;

    protected OperationalDay() {}

    private OperationalDay(LocalDate date, Vehicle vehicle, BigDecimal goal, BigDecimal odometer) {
        if (date == null || vehicle == null) throw new IllegalArgumentException("Data e veículo são obrigatórios");
        if (goal == null || goal.signum() < 0 || odometer == null || odometer.signum() < 0) {
            throw new IllegalArgumentException("Meta e odômetro não podem ser negativos");
        }
        id = UUID.randomUUID();
        this.date = date;
        this.vehicle = vehicle;
        plannedGoal = DecimalPolicy.money(goal);
        initialOdometer = DecimalPolicy.distance(odometer);
        status = OperationalDayStatus.IN_PROGRESS;
        openedAt = LocalDateTime.now();
    }

    public static OperationalDay open(LocalDate date, Vehicle vehicle, BigDecimal goal, BigDecimal odometer) {
        return new OperationalDay(date, vehicle, goal, odometer);
    }

    public void close(BigDecimal odometer) {
        if (status != OperationalDayStatus.IN_PROGRESS) throw new DomainConflictException("Dia não está em andamento");
        if (odometer == null || odometer.compareTo(initialOdometer) < 0) {
            throw new IllegalArgumentException("Odômetro final não pode ser menor que o inicial");
        }
        finalOdometer = DecimalPolicy.distance(odometer);
        status = OperationalDayStatus.CLOSED;
        closedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (status == OperationalDayStatus.CLOSED) throw new DomainConflictException("Dia fechado não pode ser cancelado");
        status = OperationalDayStatus.CANCELLED;
        closedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public LocalDate getDate() { return date; }
    public Vehicle getVehicle() { return vehicle; }
    public OperationalDayStatus getStatus() { return status; }
    public BigDecimal getPlannedGoal() { return plannedGoal; }
    public BigDecimal getInitialOdometer() { return initialOdometer; }
    public BigDecimal getFinalOdometer() { return finalOdometer; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public String getNotes() { return notes; }
}
