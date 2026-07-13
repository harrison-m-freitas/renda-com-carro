package dev.harrison.rendacomcarro.fuel.infrastructure;
import dev.harrison.rendacomcarro.fuel.domain.Fueling; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface FuelingRepository extends JpaRepository<Fueling,UUID>{ List<Fueling> findAllByOrderByFueledAtDesc(); Optional<Fueling> findFirstByVehicleIdOrderByOdometerDesc(UUID vehicleId); List<Fueling> findAllByVehicleIdAndFullTankTrueOrderByOdometerAsc(UUID vehicleId); }
