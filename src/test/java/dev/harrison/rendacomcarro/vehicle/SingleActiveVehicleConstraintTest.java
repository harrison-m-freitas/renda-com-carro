package dev.harrison.rendacomcarro.vehicle;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=vehicle-constraint-owner",
    "APP_ADMIN_PASSWORD=vehicle-constraint-credential"
})
@Transactional
class SingleActiveVehicleConstraintTest extends PostgresIntegrationTest {
    @Autowired VehicleService vehicles;
    @Autowired JdbcTemplate jdbc;

    @Test
    void databaseRejectsASecondActiveVehicle() {
        var first = vehicle("Constraint A", "DBA1A11");
        vehicle("Constraint B", "DBB2B22");

        assertThatThrownBy(() -> jdbc.update(
            "UPDATE vehicle SET status = 'ACTIVE' WHERE id = ?",
            first.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private dev.harrison.rendacomcarro.vehicle.domain.Vehicle vehicle(
        String name,
        String plate
    ) {
        return vehicles.create(new VehicleService.CreateVehicleCommand(
            name, "Toyota", "Etios", 2018, plate, FuelType.FLEX,
            BigDecimal.ZERO, BigDecimal.ZERO
        ));
    }
}
