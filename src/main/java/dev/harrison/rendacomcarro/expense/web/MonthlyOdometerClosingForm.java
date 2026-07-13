package dev.harrison.rendacomcarro.expense.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public class MonthlyOdometerClosingForm {
    @NotNull
    private UUID vehicleId;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth month;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal initialOdometer;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal finalOdometer;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal professionalKilometers;

    private boolean manualAdjustment;
    private boolean confirmWarnings;
    private String adjustmentReason;

    public String draftContextKey() {
        if (vehicleId == null || month == null) {
            return "";
        }
        return "vehicle:" + vehicleId + ":month:" + month;
    }

    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }
    public YearMonth getMonth() { return month; }
    public void setMonth(YearMonth month) { this.month = month; }
    public BigDecimal getInitialOdometer() { return initialOdometer; }
    public void setInitialOdometer(BigDecimal initialOdometer) { this.initialOdometer = initialOdometer; }
    public BigDecimal getFinalOdometer() { return finalOdometer; }
    public void setFinalOdometer(BigDecimal finalOdometer) { this.finalOdometer = finalOdometer; }
    public BigDecimal getProfessionalKilometers() { return professionalKilometers; }
    public void setProfessionalKilometers(BigDecimal professionalKilometers) {
        this.professionalKilometers = professionalKilometers;
    }
    public boolean isManualAdjustment() { return manualAdjustment; }
    public void setManualAdjustment(boolean manualAdjustment) { this.manualAdjustment = manualAdjustment; }
    public boolean isConfirmWarnings() { return confirmWarnings; }
    public void setConfirmWarnings(boolean confirmWarnings) { this.confirmWarnings = confirmWarnings; }
    public String getAdjustmentReason() { return adjustmentReason; }
    public void setAdjustmentReason(String adjustmentReason) { this.adjustmentReason = adjustmentReason; }
}
