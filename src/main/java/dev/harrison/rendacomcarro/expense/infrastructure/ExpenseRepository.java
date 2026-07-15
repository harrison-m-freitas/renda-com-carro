package dev.harrison.rendacomcarro.expense.infrastructure;

import dev.harrison.rendacomcarro.expense.domain.Expense;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    @EntityGraph(attributePaths = {"vehicle", "category"})
    List<Expense> findAllByOrderByExpenseDateDesc();

    List<Expense> findAllByCompetenceDate(LocalDate date);

    List<Expense> findAllByCompetenceDateBetween(LocalDate start, LocalDate end);

    List<Expense> findAllByPaidDateBetween(LocalDate start, LocalDate end);

    List<Expense> findAllByShiftId(UUID shiftId);

    @Query("""
        select
            e.id as id,
            v.id as vehicleId,
            v.name as vehicleName,
            c.id as categoryId,
            c.name as categoryName,
            e.competenceDate as competenceDate,
            e.paidDate as paidDate,
            e.amount as amount,
            e.classification as classification,
            e.allocationMethod as allocationMethod,
            e.professionalPercentage as professionalPercentage,
            e.professionalFixedAmount as professionalFixedAmount
        from Expense e
        left join e.vehicle v
        join e.category c
        where e.status = 'ACTIVE'
          and (v is null or v.id = :vehicleId)
          and (
              e.competenceDate between :monthStart and :monthEnd
              or (e.competenceDate < :monthStart and e.paidDate is null)
          )
        order by e.competenceDate asc, e.createdAt asc
        """)
    List<ExpenseSuggestionProjection> findSuggestionCandidates(
        @Param("monthStart") LocalDate monthStart,
        @Param("monthEnd") LocalDate monthEnd,
        @Param("vehicleId") UUID vehicleId
    );
}
