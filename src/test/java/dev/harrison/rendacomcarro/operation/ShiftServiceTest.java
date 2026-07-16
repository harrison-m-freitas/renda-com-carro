package dev.harrison.rendacomcarro.operation;

import static org.assertj.core.api.Assertions.assertThat;

import dev.harrison.rendacomcarro.operation.application.OperationalDayService;
import dev.harrison.rendacomcarro.operation.application.ShiftService;
import dev.harrison.rendacomcarro.operation.domain.Platform;
import dev.harrison.rendacomcarro.operation.domain.Shift;
import dev.harrison.rendacomcarro.operation.infrastructure.PlatformRepository;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=shift-owner",
    "APP_ADMIN_PASSWORD=shift-owner-credential"
})
class ShiftServiceTest extends PostgresIntegrationTest {
    @Autowired VehicleService vehicleService;
    @Autowired OperationalDayService dayService;
    @Autowired ShiftService shiftService;
    @Autowired PlatformRepository platformRepository;

    private UUID vehicleId;
    private UUID dayId;
    private UUID uberId;
    private LocalDateTime startTime;

    @BeforeEach
    void setUp() {
        Vehicle vehicle = vehicleService.create(new VehicleService.CreateVehicleCommand(
            "Carro dos turnos", "Toyota", "Etios", 2015,
            "S" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(),
            FuelType.FLEX, new BigDecimal("1000.0"), new BigDecimal("30000.00")));
        vehicleService.activate(vehicle.getId());
        vehicleId = vehicle.getId();
        startTime = vehicle.getCurrentOdometerRecordedAt().plusMinutes(1);
        dayId = dayService.openDay(
            startTime.toLocalDate(), vehicle.getId(), new BigDecimal("180.00"), new BigDecimal("1000.0")
        ).getId();
        uberId = platformRepository.findByCode("UBER").map(Platform::getId).orElseThrow();
    }

    @Test
    void closingShiftCalculatesMetricsAndUpdatesVehicleOdometer() {
        Shift shift = shiftService.openShift(
            dayId,
            startTime,
            new BigDecimal("1000.0"),
            "Centro",
            Set.of(uberId)
        );

        Shift closed = shiftService.closeShift(
            shift.getId(),
            startTime.plusHours(3).plusMinutes(30),
            new BigDecimal("1082.4"),
            "Pampulha",
            Set.of("Centro", "Pampulha")
        );

        assertThat(closed.getDistance()).isEqualByComparingTo("82.4");
        assertThat(closed.getDuration()).isEqualTo(Duration.ofHours(3).plusMinutes(30));
        assertThat(vehicleService.get(vehicleId).getCurrentOdometer()).isEqualByComparingTo("1082.4");
    }
}
