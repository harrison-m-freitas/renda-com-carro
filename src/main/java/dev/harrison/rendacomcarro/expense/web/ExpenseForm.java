package dev.harrison.rendacomcarro.expense.web;

import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public class ExpenseForm {
 @NotNull private UUID vehicleId; private UUID operationalDayId; private UUID shiftId; @NotNull private UUID categoryId;
 @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate expenseDate=LocalDate.now();
 @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate competenceDate=LocalDate.now();
 @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate paidDate;
 @NotNull @DecimalMin("0.01") private BigDecimal amount; @NotNull private ExpenseClassification classification=ExpenseClassification.PROFESSIONAL;
 private AllocationMethod allocationMethod; private BigDecimal professionalPercentage; private BigDecimal professionalFixedAmount; private String adjustmentReason; private String notes;
 public UUID getVehicleId(){return vehicleId;} public void setVehicleId(UUID v){vehicleId=v;} public UUID getOperationalDayId(){return operationalDayId;} public void setOperationalDayId(UUID v){operationalDayId=v;} public UUID getShiftId(){return shiftId;} public void setShiftId(UUID v){shiftId=v;} public UUID getCategoryId(){return categoryId;} public void setCategoryId(UUID v){categoryId=v;} public LocalDate getExpenseDate(){return expenseDate;} public void setExpenseDate(LocalDate v){expenseDate=v;} public LocalDate getCompetenceDate(){return competenceDate;} public void setCompetenceDate(LocalDate v){competenceDate=v;} public LocalDate getPaidDate(){return paidDate;} public void setPaidDate(LocalDate v){paidDate=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public ExpenseClassification getClassification(){return classification;} public void setClassification(ExpenseClassification v){classification=v;} public AllocationMethod getAllocationMethod(){return allocationMethod;} public void setAllocationMethod(AllocationMethod v){allocationMethod=v;} public BigDecimal getProfessionalPercentage(){return professionalPercentage;} public void setProfessionalPercentage(BigDecimal v){professionalPercentage=v;} public BigDecimal getProfessionalFixedAmount(){return professionalFixedAmount;} public void setProfessionalFixedAmount(BigDecimal v){professionalFixedAmount=v;} public String getAdjustmentReason(){return adjustmentReason;} public void setAdjustmentReason(String v){adjustmentReason=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
