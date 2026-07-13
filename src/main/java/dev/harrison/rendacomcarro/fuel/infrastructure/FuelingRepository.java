package dev.harrison.rendacomcarro.fuel.infrastructure;

import dev.harrison.rendacomcarro.fuel.domain.Fueling;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuelingRepository extends JpaRepository<Fueling, UUID> {
    List<Fueling> findAllByOrderByFueledAtDesc();
    Optional<Fueling> findFirstByVehicleIdOrderByOdometerDesc(UUID vehicleId);
    List<Fueling> findAllByVehicleIdAndFullTankTrueOrderByOdometerAsc(UUID vehicleId);
    List<Fueling> findAllByVehicleIdAndFueledAtGreaterThanEqualAndFueledAtLessThanOrderByFueledAtAsc(
        UUID vehicleId,
        LocalDateTime start,
        LocalDateTime end
    );
}
