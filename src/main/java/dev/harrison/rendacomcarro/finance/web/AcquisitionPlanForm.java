package dev.harrison.rendacomcarro.finance.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public class AcquisitionPlanForm {
    private UUID vehicleId;

    @NotBlank(message = "Informe um nome para o plano")
    @Size(max = 160)
    private String title;

    @NotNull(message = "Informe o valor total da compra")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    private BigDecimal purchaseAmount;

    @NotNull
    @DecimalMin(value = "0", message = "Os recursos próprios não podem ser negativos")
    private BigDecimal ownResourcesAmount = BigDecimal.ZERO;

    @NotNull(message = "Informe a data da compra")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate purchaseDate = LocalDate.now();

    @Size(max = 4000)
    private String notes;

    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public BigDecimal getPurchaseAmount() { return purchaseAmount; }
    public void setPurchaseAmount(BigDecimal purchaseAmount) { this.purchaseAmount = purchaseAmount; }
    public BigDecimal getOwnResourcesAmount() { return ownResourcesAmount; }
    public void setOwnResourcesAmount(BigDecimal ownResourcesAmount) { this.ownResourcesAmount = ownResourcesAmount; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
