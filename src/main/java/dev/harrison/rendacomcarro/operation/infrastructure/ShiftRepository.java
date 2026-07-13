package dev.harrison.rendacomcarro.operation.infrastructure;

import dev.harrison.rendacomcarro.operation.domain.Shift;
import dev.harrison.rendacomcarro.operation.domain.ShiftStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftRepository extends JpaRepository<Shift, UUID> {
    boolean existsByOperationalDayIdAndStatus(UUID dayId, ShiftStatus status);

    Optional<Shift> findFirstByOperationalDayIdOrderByStartedAtDesc(UUID dayId);

    @EntityGraph(attributePaths = {"platforms", "operationalDay"})
    List<Shift> findAllByOperationalDayIdOrderByStartedAtAsc(UUID dayId);

    @EntityGraph(attributePaths = {"platforms", "operationalDay", "operationalDay.vehicle"})
    @Query("""
        select distinct shift
        from Shift shift
        where shift.operationalDay.vehicle.id = :vehicleId
          and shift.startedAt >= :start
          and shift.startedAt < :end
        order by shift.startedAt asc
        """)
    List<Shift> findAllForVehicleBetween(
        @Param("vehicleId") UUID vehicleId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Override
    @EntityGraph(attributePaths = {"platforms", "operationalDay", "operationalDay.vehicle"})
    Optional<Shift> findById(UUID id);
}
