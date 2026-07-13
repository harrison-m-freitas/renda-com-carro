package dev.harrison.rendacomcarro.operation.infrastructure;

import dev.harrison.rendacomcarro.operation.domain.OperationalDay;
import dev.harrison.rendacomcarro.operation.domain.OperationalDayStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationalDayRepository extends JpaRepository<OperationalDay, UUID> {
    boolean existsByVehicleIdAndDateAndStatusNot(
        UUID vehicleId,
        LocalDate date,
        OperationalDayStatus status
    );

    @EntityGraph(attributePaths = "vehicle")
    List<OperationalDay> findAllByOrderByDateDesc();

    @EntityGraph(attributePaths = "vehicle")
    List<OperationalDay> findAllByVehicleIdAndDateBetweenOrderByDateAsc(
        UUID vehicleId,
        LocalDate start,
        LocalDate end
    );

    @Override
    @EntityGraph(attributePaths = "vehicle")
    Optional<OperationalDay> findById(UUID id);
}
