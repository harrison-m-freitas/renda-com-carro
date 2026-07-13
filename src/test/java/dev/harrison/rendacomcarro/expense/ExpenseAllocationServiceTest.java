package dev.harrison.rendacomcarro.expense;

import dev.harrison.rendacomcarro.expense.application.ExpenseAllocationService;
import dev.harrison.rendacomcarro.expense.domain.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ExpenseAllocationServiceTest {
    private final ExpenseAllocationService service = new ExpenseAllocationService();

    @Test
    void mixedExpenseUsesProfessionalMileagePercentageByDefault() {
        Expense expense = Expense.mixed(new BigDecimal("500.00"), AllocationMethod.MILEAGE_RATIO);
        MonthlyMileageSnapshot mileage = new MonthlyMileageSnapshot(new BigDecimal("2500.0"), new BigDecimal("2000.0"));
        ExpenseAllocation result = service.allocate(expense, mileage);
        assertThat(result.professionalAmount()).isEqualByComparingTo("400.00");
        assertThat(result.personalAmount()).isEqualByComparingTo("100.00");
    }
}
