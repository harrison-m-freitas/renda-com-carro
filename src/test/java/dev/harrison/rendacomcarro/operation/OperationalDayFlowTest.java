package dev.harrison.rendacomcarro.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.harrison.rendacomcarro.operation.application.OperationalDayService;
import dev.harrison.rendacomcarro.operation.application.ShiftService;
import dev.harrison.rendacomcarro.operation.domain.Platform;
import dev.harrison.rendacomcarro.operation.infrastructure.PlatformRepository;
import dev.harrison.rendacomcarro.shared.domain.DomainConflictException;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=operation-owner",
    "APP_ADMIN_PASSWORD=operation-owner-credential"
})
class OperationalDayFlowTest extends PostgresIntegrationTest {
    @Autowired VehicleService vehicleService;
    @Autowired OperationalDayService dayService;
    @Autowired ShiftService shiftService;
    @Autowired PlatformRepository platformRepository;

    @Test
    void cannotCloseDayWithOpenShift() {
        var vehicle = createVehicle("120000.0");
        var day = dayService.openDay(
            LocalDate.now(), vehicle.getId(), new BigDecimal("180.00"), new BigDecimal("120000.0")
        );
        var uberId = platformRepository.findByCode("UBER").map(Platform::getId).orElseThrow();
        shiftService.openShift(
            day.getId(),
            vehicle.getCurrentOdometerRecordedAt().plusMinutes(1),
            new BigDecimal("120000.0"),
            "Centro",
            Set.of(uberId)
        );

        assertThatThrownBy(() -> dayService.closeDay(day.getId(), new BigDecimal("120080.0")))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("turno aberto");
    }

    @Test
    void closingDayUpdatesVehicleOdometer() {
        var vehicle = createVehicle("50000.0");
        var day = dayService.openDay(
            LocalDate.now(), vehicle.getId(), new BigDecimal("180.00"), new BigDecimal("50000.0")
        );

        dayService.closeDay(day.getId(), new BigDecimal("50075.5"));

        assertThat(vehicleService.get(vehicle.getId()).getCurrentOdometer())
            .isEqualByComparingTo("50075.5");
    }

    @Test
    void closingHistoricalDayDoesNotReduceCurrentOdometer() {
        var vehicle = createVehicle("50000.0");
        var historicalDay = dayService.openDay(
            LocalDate.now().minusMonths(2),
            vehicle.getId(),
            new BigDecimal("180.00"),
            new BigDecimal("47000.0")
        );

        dayService.closeDay(historicalDay.getId(), new BigDecimal("47080.0"));

        assertThat(vehicleService.get(vehicle.getId()).getCurrentOdometer())
            .isEqualByComparingTo("50000.0");
    }

    private dev.harrison.rendacomcarro.vehicle.domain.Vehicle createVehicle(String odometer) {
        String plate = "D" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        var vehicle = vehicleService.create(new VehicleService.CreateVehicleCommand(
            "Carro do dia", "Renault", "Logan", 2018, plate, FuelType.FLEX,
            new BigDecimal(odometer), new BigDecimal("30000.00")));
        vehicleService.activateAsPrimary(vehicle.getId());
        return vehicle;
    }
}
