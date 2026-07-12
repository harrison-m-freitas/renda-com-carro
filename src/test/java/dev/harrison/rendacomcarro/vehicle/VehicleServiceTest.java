package dev.harrison.rendacomcarro.vehicle;

import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import dev.harrison.rendacomcarro.vehicle.domain.VehicleStatus;
import dev.harrison.rendacomcarro.vehicle.infrastructure.VehicleRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=vehicle-owner",
    "APP_ADMIN_PASSWORD=vehicle-owner-credential"
})
class VehicleServiceTest extends PostgresIntegrationTest {
    @Autowired VehicleService service;
    @Autowired VehicleRepository repository;

    @Test
    void activatingSecondVehicleRemovesPrimaryFlagFromFirst() {
        Vehicle first = service.create(new VehicleService.CreateVehicleCommand(
            "Etios", "Toyota", "Etios", 2015, "ABC1D23", FuelType.FLEX,
            BigDecimal.ZERO, BigDecimal.ZERO));
        Vehicle second = service.create(new VehicleService.CreateVehicleCommand(
            "Logan", "Renault", "Logan", 2018, "DEF4G56", FuelType.FLEX,
            BigDecimal.ZERO, BigDecimal.ZERO));

        service.activateAsPrimary(first.getId());
        service.activateAsPrimary(second.getId());

        assertThat(repository.findByPrimaryVehicleTrueAndStatus(VehicleStatus.ACTIVE))
            .get().extracting(Vehicle::getId).isEqualTo(second.getId());
    }
}
