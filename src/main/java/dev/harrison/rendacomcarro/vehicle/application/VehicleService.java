package dev.harrison.rendacomcarro.vehicle.application;

import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import dev.harrison.rendacomcarro.vehicle.domain.OdometerReadingSource;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import dev.harrison.rendacomcarro.vehicle.domain.VehicleStatus;
import dev.harrison.rendacomcarro.vehicle.infrastructure.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {
    private static final Set<VehicleStatus> OPERATIONAL_STATUSES = Set.of(
        VehicleStatus.ACTIVE,
        VehicleStatus.INACTIVE
    );

    private final VehicleRepository repository;
    private final VehicleOdometerService odometerService;

    public VehicleService(VehicleRepository repository, VehicleOdometerService odometerService) {
        this.repository = repository;
        this.odometerService = odometerService;
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
        List<Vehicle> operational = lockOperationalVehicles();
        operational.stream()
            .filter(vehicle -> vehicle.getStatus() == VehicleStatus.ACTIVE)
            .forEach(Vehicle::deactivate);
        repository.saveAllAndFlush(operational);

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

    @Transactional(readOnly = true)
    public Vehicle getActive(UUID id) {
        Vehicle vehicle = get(id);
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new IllegalArgumentException("Veículo ativo não encontrado");
        }
        return vehicle;
    }

    @Transactional(readOnly = true)
    public List<Vehicle> listActive() {
        return repository.findAllByStatusOrderByCreatedAtDesc(VehicleStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Optional<Vehicle> findActiveVehicle() {
        return repository.findByStatus(VehicleStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Vehicle getActiveVehicle() {
        return findActiveVehicle()
            .orElseThrow(() -> new IllegalStateException("Nenhum veículo ativo"));
    }

    @Transactional
    public Vehicle update(UUID id, UpdateVehicleCommand command) {
        Vehicle vehicle = get(id);
        vehicle.updateDetails(
            command.name(), command.make(), command.model(), command.year(), command.plate(),
            command.fuelType(), command.purchasePrice());
        repository.save(vehicle);
        odometerService.registerReading(
            id,
            command.currentOdometer(),
            LocalDateTime.now(),
            OdometerReadingSource.VEHICLE_MANUAL,
            id
        );
        return vehicle;
    }

    @Transactional
    public void activate(UUID id) {
        List<Vehicle> operational = lockOperationalVehicles();
        Vehicle selected = operational.stream()
            .filter(vehicle -> vehicle.getId().equals(id))
            .findFirst()
            .orElseGet(() -> archivedOrMissing(id));

        operational.stream()
            .filter(vehicle -> vehicle.getStatus() == VehicleStatus.ACTIVE)
            .forEach(Vehicle::deactivate);
        selected.activate();
        repository.saveAllAndFlush(operational);
    }

    @Transactional
    public void archive(UUID id) {
        List<Vehicle> operational = lockOperationalVehicles();
        Optional<Vehicle> selected = operational.stream()
            .filter(vehicle -> vehicle.getId().equals(id))
            .findFirst();
        if (selected.isEmpty()) {
            Vehicle existing = get(id);
            if (existing.getStatus() == VehicleStatus.ARCHIVED) {
                return;
            }
            throw new IllegalArgumentException("Veículo não encontrado");
        }
        selected.get().archive();
        repository.save(selected.get());
    }

    private List<Vehicle> lockOperationalVehicles() {
        return repository.findAllByStatusInForUpdate(OPERATIONAL_STATUSES);
    }

    private Vehicle archivedOrMissing(UUID id) {
        Vehicle vehicle = get(id);
        if (vehicle.getStatus() == VehicleStatus.ARCHIVED) {
            throw new IllegalStateException("Veículo arquivado não pode ser ativado.");
        }
        throw new IllegalArgumentException("Veículo não encontrado");
    }
}
