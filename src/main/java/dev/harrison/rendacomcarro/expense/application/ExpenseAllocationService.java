package dev.harrison.rendacomcarro.expense.application;

import dev.harrison.rendacomcarro.expense.domain.*;
import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class ExpenseAllocationService {
    public ExpenseAllocation allocate(Expense expense, MonthlyMileageSnapshot mileage) {
        return switch (expense.getClassification()) {
            case PROFESSIONAL -> ExpenseAllocation.allProfessional(expense.getAmount());
            case PERSONAL -> ExpenseAllocation.allPersonal(expense.getAmount());
            case MIXED -> switch (expense.getAllocationMethod()) {
                case MILEAGE_RATIO -> byPercentage(expense.getAmount(), mileage.professionalRatio());
                case MANUAL_PERCENTAGE -> byPercentage(expense.getAmount(), expense.getProfessionalPercentage());
                case FIXED_AMOUNT -> byFixedAmount(expense.getAmount(), expense.getProfessionalFixedAmount());
            };
        };
    }

    private ExpenseAllocation byPercentage(BigDecimal amount, BigDecimal ratio) {
        if (ratio == null || ratio.signum() < 0 || ratio.compareTo(BigDecimal.ONE) > 0) throw new IllegalArgumentException("Percentual profissional inválido");
        BigDecimal professional = DecimalPolicy.money(amount.multiply(ratio));
        return new ExpenseAllocation(professional, DecimalPolicy.money(amount.subtract(professional)));
    }

    private ExpenseAllocation byFixedAmount(BigDecimal amount, BigDecimal professionalFixed) {
        if (professionalFixed == null || professionalFixed.signum() < 0 || professionalFixed.compareTo(amount) > 0) throw new IllegalArgumentException("Valor profissional fixo inválido");
        return new ExpenseAllocation(DecimalPolicy.money(professionalFixed), DecimalPolicy.money(amount.subtract(professionalFixed)));
    }
}
