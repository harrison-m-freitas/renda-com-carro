package dev.harrison.rendacomcarro.vehicle.application;

import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import dev.harrison.rendacomcarro.vehicle.domain.OdometerReadingSource;
import dev.harrison.rendacomcarro.vehicle.infrastructure.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleOdometerService {
    private final VehicleRepository vehicles;

    public VehicleOdometerService(VehicleRepository vehicles) {
        this.vehicles = vehicles;
    }

    @Transactional
    public OdometerUpdateResult registerReading(
        UUID vehicleId,
        BigDecimal reading,
        LocalDateTime recordedAt,
        OdometerReadingSource source,
        UUID sourceId
    ) {
        if (vehicleId == null || reading == null || recordedAt == null || source == null) {
            throw new IllegalArgumentException("Veículo, leitura, data e origem são obrigatórios");
        }
        if (reading.signum() < 0) {
            throw new DomainValidationException("Odômetro não pode ser negativo");
        }

        var vehicle = vehicles.findById(vehicleId)
            .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado"));
        BigDecimal normalizedReading = DecimalPolicy.distance(reading);
        LocalDateTime currentRecordedAt = vehicle.getCurrentOdometerRecordedAt();

        if (currentRecordedAt != null && recordedAt.isBefore(currentRecordedAt)) {
            return OdometerUpdateResult.IGNORED_HISTORICAL;
        }

        int comparison = normalizedReading.compareTo(vehicle.getCurrentOdometer());
        if (comparison < 0) {
            throw new DomainValidationException(
                "A leitura de " + normalizedReading + " km em " + recordedAt
                    + " é menor que o odômetro atual de " + vehicle.getCurrentOdometer() + " km"
            );
        }

        if (comparison == 0) {
            if (currentRecordedAt == null || recordedAt.isAfter(currentRecordedAt)) {
                vehicle.applyCurrentOdometer(normalizedReading, recordedAt, source, sourceId);
                vehicles.save(vehicle);
            }
            return OdometerUpdateResult.IGNORED_EQUAL;
        }

        vehicle.applyCurrentOdometer(normalizedReading, recordedAt, source, sourceId);
        vehicles.save(vehicle);
        return OdometerUpdateResult.UPDATED;
    }
}
