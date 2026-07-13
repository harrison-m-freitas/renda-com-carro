package dev.harrison.rendacomcarro.expense.infrastructure;
import dev.harrison.rendacomcarro.expense.domain.MonthlyOdometerClosing; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface MonthlyOdometerClosingRepository extends JpaRepository<MonthlyOdometerClosing,UUID>{}
