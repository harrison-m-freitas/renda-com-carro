package dev.harrison.rendacomcarro.fuel.application;

import dev.harrison.rendacomcarro.fuel.domain.Fueling;
import dev.harrison.rendacomcarro.fuel.infrastructure.FuelingRepository;
import dev.harrison.rendacomcarro.vehicle.application.VehicleOdometerService;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import dev.harrison.rendacomcarro.vehicle.domain.OdometerReadingSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuelingService {
    private final FuelingRepository repository;
    private final VehicleService vehicles;
    private final VehicleOdometerService odometerService;

    public FuelingService(
        FuelingRepository repository,
        VehicleService vehicles,
        VehicleOdometerService odometerService
    ) {
        this.repository = repository;
        this.vehicles = vehicles;
        this.odometerService = odometerService;
    }

    public record CreateFuelingCommand(
        UUID vehicleId,
        LocalDateTime fueledAt,
        BigDecimal odometer,
        String station,
        FuelType fuelType,
        BigDecimal liters,
        BigDecimal pricePerLiter,
        BigDecimal totalAmount,
        boolean fullTank,
        String notes
    ) {}

    @Transactional
    public Fueling create(CreateFuelingCommand command) {
        var vehicle = vehicles.get(command.vehicleId());
        Fueling fueling = repository.save(Fueling.create(
            vehicle,
            command.fueledAt(),
            command.odometer(),
            command.station(),
            command.fuelType(),
            command.liters(),
            command.pricePerLiter(),
            command.totalAmount(),
            command.fullTank(),
            command.notes()
        ));
        odometerService.registerReading(
            vehicle.getId(),
            fueling.getOdometer(),
            fueling.getFueledAt(),
            OdometerReadingSource.FUELING,
            fueling.getId()
        );
        return fueling;
    }

    @Transactional(readOnly = true)
    public List<Fueling> listAll() {
        return repository.findAllByOrderByFueledAtDesc();
    }
}
