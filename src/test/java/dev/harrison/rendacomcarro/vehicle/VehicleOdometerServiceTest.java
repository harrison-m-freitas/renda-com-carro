package dev.harrison.rendacomcarro.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.OdometerUpdateResult;
import dev.harrison.rendacomcarro.vehicle.application.VehicleOdometerService;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import dev.harrison.rendacomcarro.vehicle.domain.OdometerReadingSource;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=odometer-owner",
    "APP_ADMIN_PASSWORD=odometer-owner-credential"
})
class VehicleOdometerServiceTest extends PostgresIntegrationTest {
    @Autowired VehicleService vehicles;
    @Autowired VehicleOdometerService odometer;

    @Test
    void newerHigherReadingUpdatesVehicleAndTraceability() {
        var vehicle = createVehicle("ODO1A23", "10000.0");
        var recordedAt = vehicle.getCurrentOdometerRecordedAt().plusHours(1);
        UUID sourceId = UUID.randomUUID();

        var result = odometer.registerReading(
            vehicle.getId(), new BigDecimal("10025.4"), recordedAt,
            OdometerReadingSource.FUELING, sourceId);

        var updated = vehicles.get(vehicle.getId());
        assertThat(result).isEqualTo(OdometerUpdateResult.UPDATED);
        assertThat(updated.getCurrentOdometer()).isEqualByComparingTo("10025.4");
        assertThat(updated.getCurrentOdometerRecordedAt())
            .isEqualTo(recordedAt.truncatedTo(ChronoUnit.MICROS));
        assertThat(updated.getCurrentOdometerSource()).isEqualTo(OdometerReadingSource.FUELING);
        assertThat(updated.getCurrentOdometerSourceId()).isEqualTo(sourceId);
    }

    @Test
    void olderHistoricalReadingDoesNotReduceCurrentOdometer() {
        var vehicle = createVehicle("ODO2A23", "10000.0");

        var result = odometer.registerReading(
            vehicle.getId(), new BigDecimal("9900.0"),
            vehicle.getCurrentOdometerRecordedAt().minusDays(2),
            OdometerReadingSource.FUELING, UUID.randomUUID());

        assertThat(result).isEqualTo(OdometerUpdateResult.IGNORED_HISTORICAL);
        assertThat(vehicles.get(vehicle.getId()).getCurrentOdometer()).isEqualByComparingTo("10000.0");
    }

    @Test
    void newerLowerReadingIsRejectedAsRegression() {
        var vehicle = createVehicle("ODO3A23", "10000.0");

        assertThatThrownBy(() -> odometer.registerReading(
            vehicle.getId(), new BigDecimal("9999.9"),
            vehicle.getCurrentOdometerRecordedAt().plusMinutes(1),
            OdometerReadingSource.SHIFT_CLOSE, UUID.randomUUID()))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("menor que o odômetro atual");
    }

    private dev.harrison.rendacomcarro.vehicle.domain.Vehicle createVehicle(String plate, String odometer) {
        return vehicles.create(new VehicleService.CreateVehicleCommand(
            "Veículo " + plate,
            "Toyota",
            "Etios",
            2018,
            plate,
            FuelType.FLEX,
            new BigDecimal(odometer),
            new BigDecimal("35000.00")
        ));
    }
}
