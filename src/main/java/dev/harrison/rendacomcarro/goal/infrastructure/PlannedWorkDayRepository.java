package dev.harrison.rendacomcarro.goal.infrastructure;
import dev.harrison.rendacomcarro.goal.domain.PlannedWorkDay; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface PlannedWorkDayRepository extends JpaRepository<PlannedWorkDay,UUID>{ List<PlannedWorkDay> findAllByMonthlyGoalIdOrderByWorkDateAsc(UUID goalId); void deleteAllByMonthlyGoalId(UUID goalId); }
