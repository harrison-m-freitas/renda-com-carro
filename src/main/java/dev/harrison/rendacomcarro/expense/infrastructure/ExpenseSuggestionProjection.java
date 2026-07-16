package dev.harrison.rendacomcarro.expense.infrastructure;

import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface ExpenseSuggestionProjection {
    UUID getId();
    UUID getVehicleId();
    String getVehicleName();
    UUID getCategoryId();
    String getCategoryName();
    LocalDate getCompetenceDate();
    LocalDate getPaidDate();
    BigDecimal getAmount();
    ExpenseClassification getClassification();
    AllocationMethod getAllocationMethod();
    BigDecimal getProfessionalPercentage();
    BigDecimal getProfessionalFixedAmount();
}
