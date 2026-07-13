package dev.harrison.rendacomcarro.expense.domain;

import dev.harrison.rendacomcarro.expense.application.OdometerOrigin;
import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
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
@Table(name = "monthly_odometer_closing")
public class MonthlyOdometerClosing {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "reference_month", nullable = false)
    private LocalDate referenceMonth;

    @Column(name = "initial_odometer", nullable = false, precision = 12, scale = 1)
    private BigDecimal initialOdometer;

    @Column(name = "final_odometer", nullable = false, precision = 12, scale = 1)
    private BigDecimal finalOdometer;

    @Column(name = "professional_kilometers", nullable = false, precision = 12, scale = 1)
    private BigDecimal professionalKilometers;

    @Column(name = "personal_kilometers", nullable = false, precision = 12, scale = 1)
    private BigDecimal personalKilometers;

    @Column(name = "professional_percentage", nullable = false, precision = 5, scale = 4)
    private BigDecimal professionalPercentage;

    @Column(name = "adjustment_reason")
    private String adjustmentReason;

    @Column(name = "manual_adjustment", nullable = false)
    private boolean manualAdjustment;

    @Column(name = "inferred_initial_odometer", precision = 12, scale = 1)
    private BigDecimal inferredInitialOdometer;

    @Column(name = "inferred_final_odometer", precision = 12, scale = 1)
    private BigDecimal inferredFinalOdometer;

    @Column(name = "inferred_professional_kilometers", precision = 12, scale = 1)
    private BigDecimal inferredProfessionalKilometers;

    @Enumerated(EnumType.STRING)
    @Column(name = "initial_odometer_origin")
    private OdometerOrigin initialOdometerOrigin;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_odometer_origin")
    private OdometerOrigin finalOdometerOrigin;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    protected MonthlyOdometerClosing() {
    }

    public static MonthlyOdometerClosing create(
        Vehicle vehicle,
        YearMonth month,
        BigDecimal initialOdometer,
        BigDecimal finalOdometer,
        BigDecimal professionalKilometers,
        String adjustmentReason
    ) {
        LocalDateTime now = LocalDateTime.now();
        return confirm(
            vehicle, month,
            initialOdometer, finalOdometer, professionalKilometers,
            null, null, now,
            initialOdometer, finalOdometer, professionalKilometers,
            adjustmentReason, now
        );
    }

    public static MonthlyOdometerClosing confirm(
        Vehicle vehicle,
        YearMonth month,
        BigDecimal inferredInitialOdometer,
        BigDecimal inferredFinalOdometer,
        BigDecimal inferredProfessionalKilometers,
        OdometerOrigin initialOrigin,
        OdometerOrigin finalOrigin,
        LocalDateTime calculatedAt,
        BigDecimal confirmedInitialOdometer,
        BigDecimal confirmedFinalOdometer,
        BigDecimal confirmedProfessionalKilometers,
        String adjustmentReason,
        LocalDateTime confirmedAt
    ) {
        if (vehicle == null || month == null || calculatedAt == null || confirmedAt == null) {
            throw new IllegalArgumentException("Veículo, mês e datas de confirmação são obrigatórios");
        }
        validateMileage(confirmedInitialOdometer, confirmedFinalOdometer, confirmedProfessionalKilometers);

        BigDecimal inferredInitial = DecimalPolicy.distance(inferredInitialOdometer);
        BigDecimal inferredFinal = DecimalPolicy.distance(inferredFinalOdometer);
        BigDecimal inferredProfessional = DecimalPolicy.distance(inferredProfessionalKilometers);
        BigDecimal initial = DecimalPolicy.distance(confirmedInitialOdometer);
        BigDecimal end = DecimalPolicy.distance(confirmedFinalOdometer);
        BigDecimal professional = DecimalPolicy.distance(confirmedProfessionalKilometers);
        BigDecimal total = end.subtract(initial);

        if (professional.compareTo(total) > 0) {
            throw new DomainValidationException(
                "Quilômetros profissionais não podem exceder o total rodado"
            );
        }

        boolean adjusted = initial.compareTo(inferredInitial) != 0
            || end.compareTo(inferredFinal) != 0
            || professional.compareTo(inferredProfessional) != 0;
        String normalizedReason = normalize(adjustmentReason);
        if (adjusted && normalizedReason == null) {
            throw new DomainValidationException(
                "A justificativa é obrigatória ao corrigir valores inferidos"
            );
        }

        MonthlyOdometerClosing closing = new MonthlyOdometerClosing();
        closing.id = UUID.randomUUID();
        closing.vehicle = vehicle;
        closing.referenceMonth = month.atDay(1);
        closing.initialOdometer = initial;
        closing.finalOdometer = end;
        closing.professionalKilometers = professional;
        closing.personalKilometers = DecimalPolicy.distance(total.subtract(professional));
        closing.professionalPercentage = total.signum() == 0
            ? BigDecimal.ZERO.setScale(4)
            : professional.divide(total, 4, RoundingMode.HALF_UP);
        closing.adjustmentReason = adjusted ? normalizedReason : null;
        closing.manualAdjustment = adjusted;
        closing.inferredInitialOdometer = inferredInitial;
        closing.inferredFinalOdometer = inferredFinal;
        closing.inferredProfessionalKilometers = inferredProfessional;
        closing.initialOdometerOrigin = initialOrigin;
        closing.finalOdometerOrigin = finalOrigin;
        closing.calculatedAt = calculatedAt;
        closing.confirmedAt = confirmedAt;
        return closing;
    }

    private static void validateMileage(
        BigDecimal initial,
        BigDecimal end,
        BigDecimal professional
    ) {
        if (initial == null || end == null || professional == null
            || initial.signum() < 0 || end.compareTo(initial) < 0
            || professional.signum() < 0) {
            throw new DomainValidationException("Quilometragens do fechamento são inválidas");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public Vehicle getVehicle() { return vehicle; }
    public LocalDate getReferenceMonth() { return referenceMonth; }
    public BigDecimal getInitialOdometer() { return initialOdometer; }
    public BigDecimal getFinalOdometer() { return finalOdometer; }
    public BigDecimal getTotalKilometers() { return finalOdometer.subtract(initialOdometer); }
    public BigDecimal getProfessionalKilometers() { return professionalKilometers; }
    public BigDecimal getPersonalKilometers() { return personalKilometers; }
    public BigDecimal getProfessionalPercentage() { return professionalPercentage; }
    public String getAdjustmentReason() { return adjustmentReason; }
    public boolean isManualAdjustment() { return manualAdjustment; }
    public BigDecimal getInferredInitialOdometer() { return inferredInitialOdometer; }
    public BigDecimal getInferredFinalOdometer() { return inferredFinalOdometer; }
    public BigDecimal getInferredProfessionalKilometers() { return inferredProfessionalKilometers; }
    public OdometerOrigin getInitialOdometerOrigin() { return initialOdometerOrigin; }
    public OdometerOrigin getFinalOdometerOrigin() { return finalOdometerOrigin; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
}
