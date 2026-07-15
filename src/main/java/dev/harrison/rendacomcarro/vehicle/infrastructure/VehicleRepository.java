package dev.harrison.rendacomcarro.vehicle.infrastructure;

import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import dev.harrison.rendacomcarro.vehicle.domain.VehicleStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    Optional<Vehicle> findByPrimaryVehicleTrueAndStatus(VehicleStatus status);

    List<Vehicle> findAllByOrderByCreatedAtDesc();

    List<Vehicle> findAllByStatusOrderByCreatedAtDesc(VehicleStatus status);

    List<Vehicle> findAllByStatusOrderByPrimaryVehicleDescNameAsc(VehicleStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.status = :status")
    List<Vehicle> findAllByStatusForUpdate(@Param("status") VehicleStatus status);
}
