package dev.harrison.rendacomcarro.expense.web;

import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public class ExpenseForm {
    private UUID vehicleId;
    private UUID operationalDayId;
    private UUID shiftId;

    @NotNull
    private UUID categoryId;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expenseDate = LocalDate.now();

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth competenceMonth = YearMonth.now();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate paidDate;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    private ExpenseClassification classification = ExpenseClassification.PROFESSIONAL;
    private AllocationMethod allocationMethod;

    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal professionalPercentagePercent;

    @DecimalMin("0")
    private BigDecimal professionalFixedAmount;
    private String adjustmentReason;
    private String notes;

    public BigDecimal professionalPercentageRatio() {
        return professionalPercentagePercent == null
            ? null
            : professionalPercentagePercent.movePointLeft(2);
    }

    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }
    public UUID getOperationalDayId() { return operationalDayId; }
    public void setOperationalDayId(UUID operationalDayId) { this.operationalDayId = operationalDayId; }
    public UUID getShiftId() { return shiftId; }
    public void setShiftId(UUID shiftId) { this.shiftId = shiftId; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public YearMonth getCompetenceMonth() { return competenceMonth; }
    public void setCompetenceMonth(YearMonth competenceMonth) { this.competenceMonth = competenceMonth; }
    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public ExpenseClassification getClassification() { return classification; }
    public void setClassification(ExpenseClassification classification) { this.classification = classification; }
    public AllocationMethod getAllocationMethod() { return allocationMethod; }
    public void setAllocationMethod(AllocationMethod allocationMethod) { this.allocationMethod = allocationMethod; }
    public BigDecimal getProfessionalPercentagePercent() { return professionalPercentagePercent; }
    public void setProfessionalPercentagePercent(BigDecimal professionalPercentagePercent) {
        this.professionalPercentagePercent = professionalPercentagePercent;
    }
    public BigDecimal getProfessionalFixedAmount() { return professionalFixedAmount; }
    public void setProfessionalFixedAmount(BigDecimal professionalFixedAmount) {
        this.professionalFixedAmount = professionalFixedAmount;
    }
    public String getAdjustmentReason() { return adjustmentReason; }
    public void setAdjustmentReason(String adjustmentReason) { this.adjustmentReason = adjustmentReason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
