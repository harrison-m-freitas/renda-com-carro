package dev.harrison.rendacomcarro.operation;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

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

    private java.util.UUID dayId;
    private java.util.UUID uberId;

    @BeforeEach
    void setUp() {
        Vehicle vehicle = vehicleService.create(new VehicleService.CreateVehicleCommand(
            "Carro dos turnos", "Toyota", "Etios", 2015, "OPS1A23", FuelType.FLEX,
            new BigDecimal("1000.0"), new BigDecimal("30000.00")));
        vehicleService.activateAsPrimary(vehicle.getId());
        dayId = dayService.openDay(
            LocalDate.of(2026, 7, 12), vehicle.getId(), new BigDecimal("180.00"), new BigDecimal("1000.0")
        ).getId();
        uberId = platformRepository.findByCode("UBER").map(Platform::getId).orElseThrow();
    }

    @Test
    void closingShiftCalculatesDistanceAndDuration() {
        Shift shift = shiftService.openShift(
            dayId,
            LocalDateTime.of(2026, 7, 12, 8, 0),
            new BigDecimal("1000.0"),
            "Centro",
            Set.of(uberId)
        );

        Shift closed = shiftService.closeShift(
            shift.getId(),
            LocalDateTime.of(2026, 7, 12, 11, 30),
            new BigDecimal("1082.4"),
            "Pampulha",
            Set.of("Centro", "Pampulha")
        );

        assertThat(closed.getDistance()).isEqualByComparingTo("82.4");
        assertThat(closed.getDuration()).isEqualTo(Duration.ofHours(3).plusMinutes(30));
    }
}
