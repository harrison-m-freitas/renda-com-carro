package dev.harrison.rendacomcarro.finance.web;

import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.domain.ObligationType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public class ObligationForm {
    @NotBlank
    private String draftKey;
    private UUID vehicleId;

    @NotNull
    private ObligationType type = ObligationType.FAMILY_LOAN;

    @NotNull
    private ObligationMode mode = ObligationMode.FLEXIBLE;

    @NotBlank
    private String creditor;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal principal;

    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal annualRatePercent = BigDecimal.ZERO;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate = LocalDate.now();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate firstDueDate;
    private Integer termMonths;

    @DecimalMin("0")
    private BigDecimal plannedInstallment;

    @DecimalMin("0")
    private BigDecimal monthlyTarget;
    private String notes;

    public String draftContextKey() {
        return draftKey == null ? "" : draftKey.trim();
    }

    public BigDecimal annualRateRatio() {
        return annualRatePercent == null
            ? BigDecimal.ZERO
            : annualRatePercent.movePointLeft(2);
    }

    public String getDraftKey() { return draftKey; }
    public void setDraftKey(String draftKey) { this.draftKey = draftKey; }
    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }
    public ObligationType getType() { return type; }
    public void setType(ObligationType type) { this.type = type; }
    public ObligationMode getMode() { return mode; }
    public void setMode(ObligationMode mode) { this.mode = mode; }
    public String getCreditor() { return creditor; }
    public void setCreditor(String creditor) { this.creditor = creditor; }
    public BigDecimal getPrincipal() { return principal; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }
    public BigDecimal getAnnualRatePercent() { return annualRatePercent; }
    public void setAnnualRatePercent(BigDecimal annualRatePercent) {
        this.annualRatePercent = annualRatePercent;
    }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getFirstDueDate() { return firstDueDate; }
    public void setFirstDueDate(LocalDate firstDueDate) { this.firstDueDate = firstDueDate; }
    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }
    public BigDecimal getPlannedInstallment() { return plannedInstallment; }
    public void setPlannedInstallment(BigDecimal plannedInstallment) {
        this.plannedInstallment = plannedInstallment;
    }
    public BigDecimal getMonthlyTarget() { return monthlyTarget; }
    public void setMonthlyTarget(BigDecimal monthlyTarget) { this.monthlyTarget = monthlyTarget; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
