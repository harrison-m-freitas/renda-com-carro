package dev.harrison.rendacomcarro.fuel;

import static org.assertj.core.api.Assertions.assertThat;

import dev.harrison.rendacomcarro.fuel.application.FuelingService;
import dev.harrison.rendacomcarro.fuel.infrastructure.FuelingRepository;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=fuel-owner",
    "APP_ADMIN_PASSWORD=fuel-owner-credential"
})
class FuelingFlowTest extends PostgresIntegrationTest {
    @Autowired VehicleService vehicles;
    @Autowired FuelingService service;
    @Autowired FuelingRepository repository;

    @Test
    void recordsFuelingAndUpdatesVehicleOdometer() {
        var vehicle = createVehicle("50000.0");
        var fueling = service.create(new FuelingService.CreateFuelingCommand(
            vehicle.getId(),
            vehicle.getCurrentOdometerRecordedAt().plusHours(1),
            new BigDecimal("50025.0"),
            "Posto",
            FuelType.FLEX,
            new BigDecimal("20.000"),
            new BigDecimal("6.000"),
            new BigDecimal("120.00"),
            true,
            null
        ));

        assertThat(fueling.getTotalAmount()).isEqualByComparingTo("120.00");
        assertThat(vehicles.get(vehicle.getId()).getCurrentOdometer()).isEqualByComparingTo("50025.0");
    }

    @Test
    void historicalFuelingIsSavedWithoutReducingCurrentOdometer() {
        var vehicle = createVehicle("50000.0");
        service.create(new FuelingService.CreateFuelingCommand(
            vehicle.getId(),
            vehicle.getCurrentOdometerRecordedAt().plusHours(2),
            new BigDecimal("50100.0"),
            "Posto atual",
            FuelType.GASOLINE,
            new BigDecimal("10.000"),
            new BigDecimal("6.000"),
            new BigDecimal("60.00"),
            true,
            null
        ));

        var historical = service.create(new FuelingService.CreateFuelingCommand(
            vehicle.getId(),
            vehicle.getCurrentOdometerRecordedAt().minusDays(1),
            new BigDecimal("49900.0"),
            "Posto histórico",
            FuelType.GASOLINE,
            new BigDecimal("10.000"),
            new BigDecimal("6.000"),
            new BigDecimal("60.00"),
            true,
            null
        ));

        assertThat(repository.findById(historical.getId())).isPresent();
        assertThat(vehicles.get(vehicle.getId()).getCurrentOdometer()).isEqualByComparingTo("50100.0");
    }

    private dev.harrison.rendacomcarro.vehicle.domain.Vehicle createVehicle(String odometer) {
        String plate = "F" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return vehicles.create(new VehicleService.CreateVehicleCommand(
            "Carro combustível", "VW", "Voyage", 2018, plate, FuelType.FLEX,
            new BigDecimal(odometer), new BigDecimal("40000.00")));
    }
}
