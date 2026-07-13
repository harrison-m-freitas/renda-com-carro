package dev.harrison.rendacomcarro.expense;

import static org.assertj.core.api.Assertions.assertThat;

import dev.harrison.rendacomcarro.expense.application.MonthlyMileageInferenceService;
import dev.harrison.rendacomcarro.expense.application.MonthlyOdometerClosingService;
import dev.harrison.rendacomcarro.expense.application.OdometerOrigin;
import dev.harrison.rendacomcarro.operation.application.OperationalDayService;
import dev.harrison.rendacomcarro.operation.application.ShiftService;
import dev.harrison.rendacomcarro.operation.domain.Platform;
import dev.harrison.rendacomcarro.operation.infrastructure.PlatformRepository;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=mileage-inference-owner",
    "APP_ADMIN_PASSWORD=mileage-inference-owner-credential"
})
class MonthlyMileageInferenceServiceTest extends PostgresIntegrationTest {
    @Autowired VehicleService vehicles;
    @Autowired OperationalDayService days;
    @Autowired ShiftService shifts;
    @Autowired PlatformRepository platforms;
    @Autowired MonthlyOdometerClosingService closings;
    @Autowired MonthlyMileageInferenceService inference;

    @Test
    void infersMonthlyMileageFromPreviousClosingAndClosedShifts() {
        var vehicle = createVehicle("I");
        var uberId = platforms.findByCode("UBER").map(Platform::getId).orElseThrow();
        YearMonth month = YearMonth.now().plusMonths(1);
        YearMonth previousMonth = month.minusMonths(1);

        closings.create(new MonthlyOdometerClosingService.CreateCommand(
            vehicle.getId(), previousMonth,
            new BigDecimal("10000.0"), new BigDecimal("10000.0"),
            BigDecimal.ZERO, null));

        var firstDay = days.openDay(
            month.atDay(1), vehicle.getId(), new BigDecimal("200.00"),
            new BigDecimal("10000.0"));
        var firstShift = shifts.openShift(
            firstDay.getId(), month.atDay(1).atTime(8, 0),
            new BigDecimal("10000.0"), "Centro", Set.of(uberId));
        shifts.closeShift(
            firstShift.getId(), month.atDay(1).atTime(10, 0),
            new BigDecimal("10030.0"), "Centro", Set.of("Centro"));
        days.closeDay(firstDay.getId(), new BigDecimal("10030.0"));

        var secondDay = days.openDay(
            month.atDay(2), vehicle.getId(), new BigDecimal("200.00"),
            new BigDecimal("10030.0"));
        var secondShift = shifts.openShift(
            secondDay.getId(), month.atDay(2).atTime(8, 0),
            new BigDecimal("10030.0"), "Pampulha", Set.of(uberId));
        shifts.closeShift(
            secondShift.getId(), month.atDay(2).atTime(10, 0),
            new BigDecimal("10050.0"), "Pampulha", Set.of("Pampulha"));
        days.closeDay(secondDay.getId(), new BigDecimal("10080.0"));

        var preview = inference.infer(vehicle.getId(), month);

        assertThat(preview.hasBlockingAlerts()).isFalse();
        assertThat(preview.inferredInitialOdometer()).isEqualByComparingTo("10000.0");
        assertThat(preview.inferredFinalOdometer()).isEqualByComparingTo("10080.0");
        assertThat(preview.totalKilometers()).isEqualByComparingTo("80.0");
        assertThat(preview.professionalKilometers()).isEqualByComparingTo("50.0");
        assertThat(preview.personalKilometers()).isEqualByComparingTo("30.0");
        assertThat(preview.professionalPercentage()).isEqualByComparingTo("0.6250");
        assertThat(preview.initialOrigin()).isEqualTo(OdometerOrigin.PREVIOUS_MONTH_CLOSING);
        assertThat(preview.finalOrigin()).isEqualTo(OdometerOrigin.CLOSED_OPERATIONAL_DAY);
        assertThat(preview.alerts()).anyMatch(alert -> alert.code().equals("DAY_SHIFT_DISTANCE_GAP"));
    }

    @Test
    void chronologicalRegressionBlocksThePreview() {
        var vehicle = createVehicle("R");
        var uberId = platforms.findByCode("UBER").map(Platform::getId).orElseThrow();
        YearMonth month = YearMonth.now().plusMonths(2);

        var firstDay = days.openDay(month.atDay(1), vehicle.getId(), new BigDecimal("200.00"),
            new BigDecimal("10000.0"));
        var firstShift = shifts.openShift(firstDay.getId(), month.atDay(1).atTime(8, 0),
            new BigDecimal("10000.0"), "Centro", Set.of(uberId));
        shifts.closeShift(firstShift.getId(), month.atDay(1).atTime(10, 0),
            new BigDecimal("10050.0"), "Centro", Set.of("Centro"));
        days.closeDay(firstDay.getId(), new BigDecimal("10050.0"));

        var secondDay = days.openDay(month.atDay(2), vehicle.getId(), new BigDecimal("200.00"),
            new BigDecimal("10040.0"));
        var secondShift = shifts.openShift(secondDay.getId(), month.atDay(2).atTime(8, 0),
            new BigDecimal("10040.0"), "Pampulha", Set.of(uberId));
        shifts.closeShift(secondShift.getId(), month.atDay(2).atTime(10, 0),
            new BigDecimal("10080.0"), "Pampulha", Set.of("Pampulha"));
        days.closeDay(secondDay.getId(), new BigDecimal("10080.0"));

        var preview = inference.infer(vehicle.getId(), month);

        assertThat(preview.hasBlockingAlerts()).isTrue();
        assertThat(preview.blockingAlerts())
            .anyMatch(alert -> alert.code().equals("ODOMETER_REGRESSION"));
    }

    private dev.harrison.rendacomcarro.vehicle.domain.Vehicle createVehicle(String prefix) {
        String plate = prefix + UUID.randomUUID().toString().replace("-", "")
            .substring(0, 6).toUpperCase();
        return vehicles.create(new VehicleService.CreateVehicleCommand(
            "Veículo da inferência", "Toyota", "Etios", 2018, plate,
            FuelType.FLEX, new BigDecimal("10000.0"), new BigDecimal("35000.00")));
    }
}
