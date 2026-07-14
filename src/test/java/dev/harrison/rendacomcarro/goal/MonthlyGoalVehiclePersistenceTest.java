package dev.harrison.rendacomcarro.goal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.harrison.rendacomcarro.goal.application.GoalService;
import dev.harrison.rendacomcarro.goal.domain.WorkloadPeriodicity;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=goal-vehicle-owner",
    "APP_ADMIN_PASSWORD=goal-vehicle-password"
})
@Transactional
class MonthlyGoalVehiclePersistenceTest extends PostgresIntegrationTest {
    @Autowired GoalService goals;
    @Autowired VehicleService vehicles;

    @Test
    void savesAndRestoresMoreThanOneVehicle() {
        Vehicle first = vehicle("Meta A", "AAA1A11");
        Vehicle second = vehicle("Meta B", "BBB2B22");

        var goal = goals.create(
            YearMonth.of(2028, 7),
            new BigDecimal("2100.00"),
            new BigDecimal("4000.00"),
            WorkloadPeriodicity.MONTHLY,
            4_800,
            Set.of(LocalDate.of(2028, 7, 1)),
            Set.of(first.getId(), second.getId())
        );

        assertThat(goals.get(goal.getId()).getVehicles())
            .extracting(Vehicle::getId)
            .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    private Vehicle vehicle(String name, String plate) {
        return vehicles.create(new VehicleService.CreateVehicleCommand(
            name, "Toyota", "Etios", 2018, plate, FuelType.FLEX,
            new BigDecimal("10000.0"), new BigDecimal("35000.00")
        ));
    }
}
