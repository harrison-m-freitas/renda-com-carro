package dev.harrison.rendacomcarro.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.harrison.rendacomcarro.expense.infrastructure.MonthlyOdometerClosingRepository;
import dev.harrison.rendacomcarro.operation.application.OperationalDayService;
import dev.harrison.rendacomcarro.operation.application.ShiftService;
import dev.harrison.rendacomcarro.operation.domain.Platform;
import dev.harrison.rendacomcarro.operation.infrastructure.PlatformRepository;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=closing-web-owner",
    "APP_ADMIN_PASSWORD=closing-web-owner-credential"
})
class MonthlyMileageClosingWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired VehicleService vehicles;
    @Autowired OperationalDayService days;
    @Autowired ShiftService shifts;
    @Autowired PlatformRepository platforms;
    @Autowired MonthlyOdometerClosingRepository closings;

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void previewShowsInferredReadonlyValuesOriginsAndRecordCounts() throws Exception {
        Scenario scenario = closedScenario();

        mvc.perform(get("/mileage-closings/new")
                .param("vehicleId", scenario.vehicleId().toString())
                .param("month", scenario.month().toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Prévia calculada")))
            .andExpect(content().string(containsString("Primeiro dia operacional do mês")))
            .andExpect(content().string(containsString("Último dia operacional fechado")))
            .andExpect(content().string(containsString("1 dia fechado")))
            .andExpect(content().string(containsString("1 turno fechado")))
            .andExpect(content().string(containsString("readonly")))
            .andExpect(content().string(containsString("Corrigir valores")));
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void postingPreviewWithoutChangesCreatesAutomaticClosing() throws Exception {
        Scenario scenario = closedScenario();

        mvc.perform(post("/mileage-closings")
                .with(csrf())
                .param("vehicleId", scenario.vehicleId().toString())
                .param("month", scenario.month().toString())
                .param("manualAdjustment", "false")
                .param("initialOdometer", "10000.0")
                .param("finalOdometer", "10030.0")
                .param("professionalKilometers", "30.0")
                .param("confirmWarnings", "true"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/mileage-closings"));

        var closing = closings.findByVehicleIdAndReferenceMonth(
            scenario.vehicleId(), scenario.month().atDay(1)).orElseThrow();
        assertThat(closing.isManualAdjustment()).isFalse();
        assertThat(closing.getInferredProfessionalKilometers()).isEqualByComparingTo("30.0");
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void manualChangeWithoutReasonReturnsThePreviewWithValidationError() throws Exception {
        Scenario scenario = closedScenario();

        mvc.perform(post("/mileage-closings")
                .with(csrf())
                .param("vehicleId", scenario.vehicleId().toString())
                .param("month", scenario.month().toString())
                .param("manualAdjustment", "true")
                .param("initialOdometer", "10000.0")
                .param("finalOdometer", "10030.0")
                .param("professionalKilometers", "25.0")
                .param("adjustmentReason", "")
                .param("confirmWarnings", "true"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("justificativa")))
            .andExpect(content().string(containsString("Prévia calculada")));
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void blockingPreviewDoesNotOfferConfirmation() throws Exception {
        var vehicle = createVehicle();
        YearMonth month = YearMonth.now().plusMonths(1);
        days.openDay(month.atDay(1), vehicle.getId(), new BigDecimal("200.00"),
            new BigDecimal("10000.0"));

        mvc.perform(get("/mileage-closings/new")
                .param("vehicleId", vehicle.getId().toString())
                .param("month", month.toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Existe dia operacional em andamento")))
            .andExpect(content().string(containsString("Fechamento bloqueado")));
    }

    private Scenario closedScenario() {
        var vehicle = createVehicle();
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

    private dev.harrison.rendacomcarro.vehicle.domain.Vehicle createVehicle() {
        String plate = "W" + UUID.randomUUID().toString().replace("-", "")
            .substring(0, 6).toUpperCase();
        return vehicles.create(new VehicleService.CreateVehicleCommand(
            "Veículo web", "Toyota", "Etios", 2018, plate,
            FuelType.FLEX, new BigDecimal("10000.0"), new BigDecimal("35000.00")));
    }

    private record Scenario(UUID vehicleId, YearMonth month) {}
}
