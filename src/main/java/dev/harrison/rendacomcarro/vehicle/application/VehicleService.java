package dev.harrison.rendacomcarro.vehicle.application;

import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import dev.harrison.rendacomcarro.vehicle.domain.VehicleStatus;
import dev.harrison.rendacomcarro.vehicle.infrastructure.VehicleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {
    private final VehicleRepository repository;

    public VehicleService(VehicleRepository repository) {
        this.repository = repository;
    }

    public record CreateVehicleCommand(
        String name,
        String make,
        String model,
        int year,
        String plate,
        FuelType fuelType,
        BigDecimal initialOdometer,
        BigDecimal purchasePrice
    ) {
    }

    public record UpdateVehicleCommand(
        String name,
        String make,
        String model,
        int year,
        String plate,
        FuelType fuelType,
        BigDecimal currentOdometer,
        BigDecimal purchasePrice
    ) {
    }

    @Transactional
    public Vehicle create(CreateVehicleCommand command) {
        Vehicle vehicle = Vehicle.create(
            command.name(), command.make(), command.model(), command.year(), command.plate(),
            command.fuelType(), command.initialOdometer(), command.purchasePrice());
        return repository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> listAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Vehicle get(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado"));
    }

    @Transactional
    public Vehicle update(UUID id, UpdateVehicleCommand command) {
        Vehicle vehicle = get(id);
        vehicle.update(
            command.name(), command.make(), command.model(), command.year(), command.plate(),
            command.fuelType(), command.currentOdometer(), command.purchasePrice());
        return repository.save(vehicle);
    }

    @Transactional
    public void activateAsPrimary(UUID id) {
        List<Vehicle> activeVehicles = repository.findAllByStatusForUpdate(VehicleStatus.ACTIVE);
        Vehicle selected = activeVehicles.stream()
            .filter(vehicle -> vehicle.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Veículo ativo não encontrado"));

        activeVehicles.forEach(Vehicle::clearPrimary);
        selected.activateAsPrimary();
        repository.saveAll(activeVehicles);
    }

    @Transactional
    public void archive(UUID id) {
        Vehicle vehicle = get(id);
        vehicle.archive();
        repository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public Vehicle getPrimaryVehicle() {
        return repository.findByPrimaryVehicleTrueAndStatus(VehicleStatus.ACTIVE)
            .orElseThrow(() -> new IllegalStateException("Nenhum veículo principal ativo"));
    }
}
