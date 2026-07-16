package dev.harrison.rendacomcarro.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=vehicle-owner",
    "APP_ADMIN_PASSWORD=vehicle-owner-credential"
})
@Transactional
class VehicleServiceTest extends PostgresIntegrationTest {
    @Autowired VehicleService service;
    @Autowired VehicleRepository repository;

    @Test
    void creatingSecondVehicleInactivatesCurrentVehicleAndActivatesNewVehicle() {
        Vehicle first = vehicle("Etios", "ABC1D23");
        Vehicle second = vehicle("Logan", "DEF4G56");

        assertThat(repository.findById(first.getId()).orElseThrow().getStatus())
            .isEqualTo(VehicleStatus.INACTIVE);
        assertThat(repository.findById(second.getId()).orElseThrow().getStatus())
            .isEqualTo(VehicleStatus.ACTIVE);
        assertThat(service.getActiveVehicle().getId()).isEqualTo(second.getId());
    }

    @Test
    void activatingInactiveVehicleInactivatesCurrentVehicle() {
        Vehicle first = vehicle("Etios", "GHI7J89");
        Vehicle second = vehicle("Logan", "KLM1N23");

        service.activate(first.getId());

        assertThat(repository.findById(first.getId()).orElseThrow().getStatus())
            .isEqualTo(VehicleStatus.ACTIVE);
        assertThat(repository.findById(second.getId()).orElseThrow().getStatus())
            .isEqualTo(VehicleStatus.INACTIVE);
    }

    @Test
    void archivedVehicleCannotBeActivated() {
        Vehicle first = vehicle("Etios", "OPQ4R56");
        vehicle("Logan", "STU7V89");
        service.archive(first.getId());

        assertThatThrownBy(() -> service.activate(first.getId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Veículo arquivado não pode ser ativado.");
    }

    @Test
    void activeVehicleCannotBeArchivedUntilAnotherVehicleIsActivated() {
        Vehicle active = vehicle("Etios", "WXY1Z23");

        assertThatThrownBy(() -> service.archive(active.getId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Ative outro veículo antes de arquivar o veículo atual.");
    }

    @Test
    void inactiveVehicleCanBeArchived() {
        Vehicle inactive = vehicle("Etios", "BRA2S34");
        vehicle("Logan", "CAR5R67");

        service.archive(inactive.getId());

        assertThat(repository.findById(inactive.getId()).orElseThrow().getStatus())
            .isEqualTo(VehicleStatus.ARCHIVED);
    }

    private Vehicle vehicle(String name, String plate) {
        return service.create(new VehicleService.CreateVehicleCommand(
            name, "Toyota", name, 2018, plate, FuelType.FLEX,
            BigDecimal.ZERO, BigDecimal.ZERO
        ));
    }
}
