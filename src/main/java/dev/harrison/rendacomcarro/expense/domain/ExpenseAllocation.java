package dev.harrison.rendacomcarro.expense.domain;

import java.math.BigDecimal;

public record ExpenseAllocation(BigDecimal professionalAmount, BigDecimal personalAmount) {
    public static ExpenseAllocation allProfessional(BigDecimal amount) {
        return new ExpenseAllocation(amount, BigDecimal.ZERO.setScale(2));
    }
    public static ExpenseAllocation allPersonal(BigDecimal amount) {
        return new ExpenseAllocation(BigDecimal.ZERO.setScale(2), amount);
    }
}
