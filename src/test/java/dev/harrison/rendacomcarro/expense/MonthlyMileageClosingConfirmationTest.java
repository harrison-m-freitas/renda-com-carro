package dev.harrison.rendacomcarro.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.harrison.rendacomcarro.expense.application.MonthlyOdometerClosingService;
import dev.harrison.rendacomcarro.operation.application.OperationalDayService;
import dev.harrison.rendacomcarro.operation.application.ShiftService;
import dev.harrison.rendacomcarro.operation.domain.Platform;
import dev.harrison.rendacomcarro.operation.infrastructure.PlatformRepository;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=closing-confirmation-owner",
    "APP_ADMIN_PASSWORD=closing-confirmation-owner-credential"
})
class MonthlyMileageClosingConfirmationTest extends PostgresIntegrationTest {
    @Autowired VehicleService vehicles;
    @Autowired OperationalDayService days;
    @Autowired ShiftService shifts;
    @Autowired PlatformRepository platforms;
    @Autowired MonthlyOdometerClosingService closings;

    @Test
    void automaticConfirmationPersistsInferenceAndUpdatesVehicleOdometer() {
        Scenario scenario = closedScenario("A");

        var closing = closings.confirm(new MonthlyOdometerClosingService.ConfirmCommand(
            scenario.vehicleId(), scenario.month(), false,
            null, null, null, null, true));

        assertThat(closing.isManualAdjustment()).isFalse();
        assertThat(closing.getInitialOdometer()).isEqualByComparingTo("10000.0");
        assertThat(closing.getFinalOdometer()).isEqualByComparingTo("10030.0");
        assertThat(closing.getProfessionalKilometers()).isEqualByComparingTo("30.0");
        assertThat(closing.getInferredInitialOdometer()).isEqualByComparingTo("10000.0");
        assertThat(closing.getInferredFinalOdometer()).isEqualByComparingTo("10030.0");
        assertThat(closing.getInitialOdometerOrigin()).isNotNull();
        assertThat(closing.getFinalOdometerOrigin()).isNotNull();
        assertThat(closing.getCalculatedAt()).isNotNull();
        assertThat(closing.getConfirmedAt()).isNotNull();
        assertThat(vehicles.get(scenario.vehicleId()).getCurrentOdometer())
            .isEqualByComparingTo("10030.0");
    }

    @Test
    void changedValuesRequireAJustification() {
        Scenario scenario = closedScenario("B");

        assertThatThrownBy(() -> closings.confirm(
            new MonthlyOdometerClosingService.ConfirmCommand(
                scenario.vehicleId(), scenario.month(), true,
                new BigDecimal("10000.0"), new BigDecimal("10030.0"),
                new BigDecimal("25.0"), " ", true)))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("justificativa");
    }

    @Test
    void manualCorrectionPersistsOriginalAndConfirmedValues() {
        Scenario scenario = closedScenario("C");

        var closing = closings.confirm(new MonthlyOdometerClosingService.ConfirmCommand(
            scenario.vehicleId(), scenario.month(), true,
            new BigDecimal("10000.0"), new BigDecimal("10030.0"),
            new BigDecimal("25.0"), "Uma corrida pessoal ocorreu durante o turno.", true));

        assertThat(closing.isManualAdjustment()).isTrue();
        assertThat(closing.getProfessionalKilometers()).isEqualByComparingTo("25.0");
        assertThat(closing.getInferredProfessionalKilometers()).isEqualByComparingTo("30.0");
        assertThat(closing.getAdjustmentReason()).contains("corrida pessoal");
    }

    @Test
    void blockingPreviewCannotBeConfirmed() {
        var vehicle = createVehicle("D");
        YearMonth month = YearMonth.now().plusMonths(1);
        days.openDay(month.atDay(2), vehicle.getId(),
            new BigDecimal("200.00"), new BigDecimal("10000.0"));

        assertThatThrownBy(() -> closings.confirm(
            new MonthlyOdometerClosingService.ConfirmCommand(
                vehicle.getId(), month, false, null, null, null, null, true)))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("dia operacional");
    }

    private Scenario closedScenario(String suffix) {
        var vehicle = createVehicle(suffix);
        YearMonth month = YearMonth.now().plusMonths(1);
        LocalDate date = month.atDay(2);
        var day = days.openDay(date, vehicle.getId(), new BigDecimal("200.00"),
            new BigDecimal("10000.0"));
        UUID uberId = platforms.findByCode("UBER").map(Platform::getId).orElseThrow();
        var shift = shifts.openShift(day.getId(), date.atTime(8, 0),
            new BigDecimal("10000.0"), "Centro", Set.of(uberId));
        shifts.closeShift(shift.getId(), date.atTime(10, 0),
            new BigDecimal("10030.0"), "Centro", Set.of("Centro"));
        days.closeDay(day.getId(), new BigDecimal("10030.0"));
        return new Scenario(vehicle.getId(), month);
    }

    private dev.harrison.rendacomcarro.vehicle.domain.Vehicle createVehicle(String suffix) {
        String plate = suffix + UUID.randomUUID().toString().replace("-", "")
            .substring(0, 6).toUpperCase();
        return vehicles.create(new VehicleService.CreateVehicleCommand(
            "Veículo " + suffix, "Toyota", "Etios", 2018, plate,
            FuelType.FLEX, new BigDecimal("10000.0"), new BigDecimal("35000.00")));
    }

    private record Scenario(UUID vehicleId, YearMonth month) {}
}
