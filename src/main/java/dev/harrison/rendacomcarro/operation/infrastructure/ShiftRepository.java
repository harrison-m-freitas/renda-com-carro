package dev.harrison.rendacomcarro.operation.infrastructure;
import dev.harrison.rendacomcarro.operation.domain.*; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ShiftRepository extends JpaRepository<Shift,UUID>{ boolean existsByOperationalDayIdAndStatus(UUID dayId,ShiftStatus status); Optional<Shift> findFirstByOperationalDayIdOrderByStartedAtDesc(UUID dayId); List<Shift> findAllByOperationalDayIdOrderByStartedAtAsc(UUID dayId); }
