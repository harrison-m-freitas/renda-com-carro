package dev.harrison.rendacomcarro.expense.infrastructure;

import dev.harrison.rendacomcarro.expense.domain.MonthlyOdometerClosing;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyOdometerClosingRepository extends JpaRepository<MonthlyOdometerClosing, UUID> {
    Optional<MonthlyOdometerClosing> findByVehicleIdAndReferenceMonth(
        UUID vehicleId,
        LocalDate referenceMonth
    );

    @EntityGraph(attributePaths = "vehicle")
    Optional<MonthlyOdometerClosing> findTopByVehicleIdAndReferenceMonthBeforeOrderByReferenceMonthDesc(
        UUID vehicleId,
        LocalDate referenceMonth
    );

    @EntityGraph(attributePaths = "vehicle")
    List<MonthlyOdometerClosing> findAllByOrderByReferenceMonthDesc();

    @EntityGraph(attributePaths = "vehicle")
    List<MonthlyOdometerClosing>
        findAllByVehicleIdAndReferenceMonthLessThanEqualOrderByReferenceMonthDesc(
            UUID vehicleId,
            LocalDate maximumMonth
        );
}
