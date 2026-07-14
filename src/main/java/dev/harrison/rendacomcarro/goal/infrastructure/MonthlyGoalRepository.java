package dev.harrison.rendacomcarro.goal.infrastructure;

import dev.harrison.rendacomcarro.goal.domain.MonthlyGoal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyGoalRepository extends JpaRepository<MonthlyGoal, UUID> {
    @EntityGraph(attributePaths = "vehicles")
    Optional<MonthlyGoal> findByReferenceMonth(LocalDate referenceMonth);

    @Override
    @EntityGraph(attributePaths = "vehicles")
    Optional<MonthlyGoal> findById(UUID id);
}
