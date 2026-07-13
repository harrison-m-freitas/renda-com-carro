package dev.harrison.rendacomcarro.expense.application;

import dev.harrison.rendacomcarro.expense.domain.MonthlyOdometerClosing;
import dev.harrison.rendacomcarro.expense.infrastructure.MonthlyOdometerClosingRepository;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonthlyOdometerClosingService {
    private final MonthlyOdometerClosingRepository repository;
    private final VehicleService vehicles;

    public MonthlyOdometerClosingService(
        MonthlyOdometerClosingRepository repository,
        VehicleService vehicles
    ) {
        this.repository = repository;
        this.vehicles = vehicles;
    }

    public record CreateCommand(
        UUID vehicleId,
        YearMonth month,
        BigDecimal initialOdometer,
        BigDecimal finalOdometer,
        BigDecimal professionalKilometers,
        String adjustmentReason
    ) {}

    @Transactional
    public MonthlyOdometerClosing create(CreateCommand command) {
        if (command == null || command.vehicleId() == null || command.month() == null) {
            throw new IllegalArgumentException("Veículo e mês são obrigatórios");
        }
        if (repository.findByVehicleIdAndReferenceMonth(
            command.vehicleId(), command.month().atDay(1)).isPresent()) {
            throw new IllegalArgumentException(
                "Já existe fechamento de quilometragem para o veículo neste mês"
            );
        }
        var vehicle = vehicles.get(command.vehicleId());
        return repository.save(MonthlyOdometerClosing.create(
            vehicle,
            command.month(),
            command.initialOdometer(),
            command.finalOdometer(),
            command.professionalKilometers(),
            command.adjustmentReason()
        ));
    }

    @Transactional(readOnly = true)
    public List<MonthlyOdometerClosing> listAll() {
        return repository.findAllByOrderByReferenceMonthDesc();
    }
}
