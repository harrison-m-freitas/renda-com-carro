package dev.harrison.rendacomcarro.goal.infrastructure;
import dev.harrison.rendacomcarro.goal.domain.MonthlyGoal; import java.time.LocalDate; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface MonthlyGoalRepository extends JpaRepository<MonthlyGoal,UUID>{ Optional<MonthlyGoal> findByReferenceMonth(LocalDate referenceMonth); }
