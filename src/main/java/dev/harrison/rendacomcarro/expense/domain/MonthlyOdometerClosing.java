package dev.harrison.rendacomcarro.expense.domain;

import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
        if (vehicle == null || month == null) {
            throw new IllegalArgumentException("Veículo e mês são obrigatórios");
        }
        if (initialOdometer == null || finalOdometer == null || professionalKilometers == null
            || initialOdometer.signum() < 0 || finalOdometer.compareTo(initialOdometer) < 0
            || professionalKilometers.signum() < 0) {
            throw new IllegalArgumentException("Quilometragens do fechamento são inválidas");
        }

        BigDecimal initial = DecimalPolicy.distance(initialOdometer);
        BigDecimal end = DecimalPolicy.distance(finalOdometer);
        BigDecimal total = end.subtract(initial);
        BigDecimal professional = DecimalPolicy.distance(professionalKilometers);
        if (professional.compareTo(total) > 0) {
            throw new IllegalArgumentException(
                "Quilômetros profissionais não podem exceder o total rodado"
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
        closing.adjustmentReason = adjustmentReason == null || adjustmentReason.isBlank()
            ? null
            : adjustmentReason.trim();
        return closing;
    }

    public UUID getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public LocalDate getReferenceMonth() {
        return referenceMonth;
    }

    public BigDecimal getInitialOdometer() {
        return initialOdometer;
    }

    public BigDecimal getFinalOdometer() {
        return finalOdometer;
    }

    public BigDecimal getTotalKilometers() {
        return finalOdometer.subtract(initialOdometer);
    }

    public BigDecimal getProfessionalKilometers() {
        return professionalKilometers;
    }

    public BigDecimal getPersonalKilometers() {
        return personalKilometers;
    }

    public BigDecimal getProfessionalPercentage() {
        return professionalPercentage;
    }

    public String getAdjustmentReason() {
        return adjustmentReason;
    }
}
