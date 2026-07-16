package dev.harrison.rendacomcarro.finance.domain;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "acquisition_plan")
public class AcquisitionPlan {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "purchase_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal purchaseAmount;

    @Column(name = "own_resources_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal ownResourcesAmount;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AcquisitionPlan() {
    }

    public static AcquisitionPlan create(
        Vehicle vehicle,
        String title,
        BigDecimal purchaseAmount,
        BigDecimal ownResourcesAmount,
        LocalDate purchaseDate,
        String notes
    ) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Informe um nome para o plano de compra");
        }
        if (purchaseAmount == null || purchaseAmount.signum() <= 0) {
            throw new IllegalArgumentException("O valor da compra deve ser maior que zero");
        }
        BigDecimal own = ownResourcesAmount == null ? BigDecimal.ZERO : ownResourcesAmount;
        if (own.signum() < 0 || own.compareTo(purchaseAmount) > 0) {
            throw new IllegalArgumentException("Os recursos próprios devem estar entre zero e o valor da compra");
        }
        if (purchaseDate == null) {
            throw new IllegalArgumentException("Informe a data da compra");
        }

        AcquisitionPlan plan = new AcquisitionPlan();
        plan.id = UUID.randomUUID();
        plan.vehicle = vehicle;
        plan.title = title.trim();
        plan.purchaseAmount = DecimalPolicy.money(purchaseAmount);
        plan.ownResourcesAmount = DecimalPolicy.money(own);
        plan.purchaseDate = purchaseDate;
        plan.notes = normalizeNotes(notes);
        plan.createdAt = LocalDateTime.now();
        return plan;
    }

    private static String normalizeNotes(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public Vehicle getVehicle() { return vehicle; }
    public String getTitle() { return title; }
    public BigDecimal getPurchaseAmount() { return purchaseAmount; }
    public BigDecimal getOwnResourcesAmount() { return ownResourcesAmount; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
