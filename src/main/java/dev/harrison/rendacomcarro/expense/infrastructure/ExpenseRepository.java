package dev.harrison.rendacomcarro.expense.infrastructure;
import dev.harrison.rendacomcarro.expense.domain.Expense; import java.time.LocalDate; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ExpenseRepository extends JpaRepository<Expense,UUID>{ List<Expense> findAllByOrderByExpenseDateDesc(); List<Expense> findAllByCompetenceDate(LocalDate date); }
