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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        var vehicle = vehicles.create(new VehicleService.CreateVehicleCommand(
            "Veículo da inferência", "Toyota", "Etios", 2018,
            "I" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(),
            FuelType.FLEX, new BigDecimal("10000.0"), new BigDecimal("35000.00")));
        var uberId = platforms.findByCode("UBER").map(Platform::getId).orElseThrow();

        closings.create(new MonthlyOdometerClosingService.CreateCommand(
            vehicle.getId(), YearMonth.of(2026, 6),
            new BigDecimal("10000.0"), new BigDecimal("10000.0"),
            BigDecimal.ZERO, null));

        var firstDay = days.openDay(
            LocalDate.of(2026, 7, 1), vehicle.getId(), new BigDecimal("200.00"),
            new BigDecimal("10000.0"));
        var firstShift = shifts.openShift(
            firstDay.getId(), LocalDateTime.of(2026, 7, 1, 8, 0),
            new BigDecimal("10000.0"), "Centro", Set.of(uberId));
        shifts.closeShift(
            firstShift.getId(), LocalDateTime.of(2026, 7, 1, 10, 0),
            new BigDecimal("10030.0"), "Centro", Set.of("Centro"));
        days.closeDay(firstDay.getId(), new BigDecimal("10030.0"));

        var secondDay = days.openDay(
            LocalDate.of(2026, 7, 2), vehicle.getId(), new BigDecimal("200.00"),
            new BigDecimal("10030.0"));
        var secondShift = shifts.openShift(
            secondDay.getId(), LocalDateTime.of(2026, 7, 2, 8, 0),
            new BigDecimal("10030.0"), "Pampulha", Set.of(uberId));
        shifts.closeShift(
            secondShift.getId(), LocalDateTime.of(2026, 7, 2, 10, 0),
            new BigDecimal("10050.0"), "Pampulha", Set.of("Pampulha"));
        days.closeDay(secondDay.getId(), new BigDecimal("10080.0"));

        var preview = inference.infer(vehicle.getId(), YearMonth.of(2026, 7));

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
}
