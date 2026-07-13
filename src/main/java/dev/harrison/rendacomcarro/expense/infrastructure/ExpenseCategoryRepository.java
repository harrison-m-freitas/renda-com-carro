package dev.harrison.rendacomcarro.expense.infrastructure;
import dev.harrison.rendacomcarro.expense.domain.ExpenseCategory; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory,UUID>{ List<ExpenseCategory> findAllByActiveTrueOrderByNameAsc(); }
