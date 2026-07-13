package dev.harrison.rendacomcarro.operation.infrastructure;
import dev.harrison.rendacomcarro.operation.domain.*; import java.time.LocalDate; import java.util.*; import org.springframework.data.jpa.repository.*;
public interface OperationalDayRepository extends JpaRepository<OperationalDay,UUID>{ boolean existsByVehicleIdAndDateAndStatusNot(UUID vehicleId,LocalDate date,OperationalDayStatus status); @EntityGraph(attributePaths="vehicle") List<OperationalDay> findAllByOrderByDateDesc(); @Override @EntityGraph(attributePaths="vehicle") Optional<OperationalDay> findById(UUID id); }
