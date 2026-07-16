package dev.harrison.rendacomcarro.expense.application;

import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.Expense;
import dev.harrison.rendacomcarro.expense.domain.ExpenseAllocation;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import dev.harrison.rendacomcarro.expense.domain.MonthlyMileageSnapshot;
import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class ExpenseAllocationService {
    public ExpenseAllocation allocate(Expense expense, MonthlyMileageSnapshot mileage) {
        return allocate(
            expense.getAmount(),
            expense.getClassification(),
            expense.getAllocationMethod(),
            expense.getProfessionalPercentage(),
            expense.getProfessionalFixedAmount(),
            mileage
        );
    }

    public ExpenseAllocation allocate(
        BigDecimal amount,
        ExpenseClassification classification,
        AllocationMethod method,
        BigDecimal professionalPercentage,
        BigDecimal professionalFixedAmount,
        MonthlyMileageSnapshot mileage
    ) {
        if (amount == null || classification == null) {
            throw new IllegalArgumentException("Gasto e classificação são obrigatórios");
        }
        return switch (classification) {
            case PROFESSIONAL -> ExpenseAllocation.allProfessional(DecimalPolicy.money(amount));
            case PERSONAL -> ExpenseAllocation.allPersonal(DecimalPolicy.money(amount));
            case MIXED -> {
                if (method == null) {
                    throw new IllegalArgumentException("Método de rateio é obrigatório");
                }
                yield switch (method) {
                    case MILEAGE_RATIO -> {
                        if (mileage == null) {
                            throw new IllegalArgumentException(
                                "Percentual profissional de quilometragem indisponível"
                            );
                        }
                        yield byPercentage(amount, mileage.professionalRatio());
                    }
                    case MANUAL_PERCENTAGE -> byPercentage(amount, professionalPercentage);
                    case FIXED_AMOUNT -> byFixedAmount(amount, professionalFixedAmount);
                };
            }
        };
    }

    private ExpenseAllocation byPercentage(BigDecimal amount, BigDecimal ratio) {
        if (ratio == null || ratio.signum() < 0 || ratio.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Percentual profissional inválido");
        }
        BigDecimal professional = DecimalPolicy.money(amount.multiply(ratio));
        return new ExpenseAllocation(
            professional,
            DecimalPolicy.money(amount.subtract(professional))
        );
    }

    private ExpenseAllocation byFixedAmount(BigDecimal amount, BigDecimal professionalFixed) {
        if (professionalFixed == null || professionalFixed.signum() < 0
            || professionalFixed.compareTo(amount) > 0) {
            throw new IllegalArgumentException("Valor profissional fixo inválido");
        }
        return new ExpenseAllocation(
            DecimalPolicy.money(professionalFixed),
            DecimalPolicy.money(amount.subtract(professionalFixed))
        );
    }
}
