package dev.harrison.rendacomcarro.expense.infrastructure;

import dev.harrison.rendacomcarro.expense.domain.Expense;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findAllByOrderByExpenseDateDesc();

    List<Expense> findAllByCompetenceDate(LocalDate date);

    List<Expense> findAllByCompetenceDateBetween(LocalDate start, LocalDate end);

    List<Expense> findAllByPaidDateBetween(LocalDate start, LocalDate end);

    List<Expense> findAllByShiftId(UUID shiftId);
}
