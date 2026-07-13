package dev.harrison.rendacomcarro.operation.infrastructure;
import dev.harrison.rendacomcarro.operation.domain.*; import java.time.LocalDate; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface OperationalDayRepository extends JpaRepository<OperationalDay,UUID>{ boolean existsByVehicleIdAndDateAndStatusNot(UUID vehicleId,LocalDate date,OperationalDayStatus status); List<OperationalDay> findAllByOrderByDateDesc(); }
