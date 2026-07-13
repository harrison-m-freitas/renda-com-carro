package dev.harrison.rendacomcarro.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.application.FormDraftService.SaveDraftCommand;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.expense.application.MonthlyClosingFormSubmissionService;
import dev.harrison.rendacomcarro.expense.infrastructure.MonthlyOdometerClosingRepository;
import dev.harrison.rendacomcarro.expense.web.MonthlyOdometerClosingForm;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=closing-submission-owner",
    "APP_ADMIN_PASSWORD=closing-submission-password"
})
@Transactional
class MonthlyClosingFormSubmissionServiceTest extends PostgresIntegrationTest {
    @Autowired MonthlyClosingFormSubmissionService submissions;
    @Autowired FormDraftService drafts;
    @Autowired MonthlyOdometerClosingRepository closings;
    @Autowired VehicleService vehicles;
    @Autowired OperationalDayService days;
    @Autowired ShiftService shifts;
    @Autowired PlatformRepository platforms;
    @Autowired ObjectMapper mapper;

    @Test
    void confirmsClosingAndDeletesExactVehicleMonthDraft() {
        Scenario scenario = closedScenario();
        MonthlyOdometerClosingForm form = automaticForm(scenario);
        seedDraft(form, false, null);

        submissions.submit("closing-submission-owner", form);

        assertThat(closings.findByVehicleIdAndReferenceMonth(
            scenario.vehicleId(), scenario.month().atDay(1)
        )).isPresent();
        assertThat(drafts.find(
            "closing-submission-owner",
            FormDraftType.MILEAGE_CLOSING,
            form.draftContextKey()
        )).isEmpty();
    }

    @Test
    void failedConfirmationPreservesDraft() {
        Scenario scenario = closedScenario();
        MonthlyOdometerClosingForm form = automaticForm(scenario);
        form.setManualAdjustment(true);
        form.setProfessionalKilometers(new BigDecimal("25.0"));
        form.setAdjustmentReason(null);
        seedDraft(form, true, "Correção guardada apenas no rascunho");

        assertThatThrownBy(() -> submissions.submit("closing-submission-owner", form))
            .isInstanceOf(DomainValidationException.class);
        assertThat(drafts.find(
            "closing-submission-owner",
            FormDraftType.MILEAGE_CLOSING,
            form.draftContextKey()
        )).isPresent();
    }

    private MonthlyOdometerClosingForm automaticForm(Scenario scenario) {
        MonthlyOdometerClosingForm form = new MonthlyOdometerClosingForm();
        form.setVehicleId(scenario.vehicleId());
        form.setMonth(scenario.month());
        form.setInitialOdometer(new BigDecimal("10000.0"));
        form.setFinalOdometer(new BigDecimal("10030.0"));
        form.setProfessionalKilometers(new BigDecimal("30.0"));
        form.setConfirmWarnings(true);
        return form;
    }

    private void seedDraft(
        MonthlyOdometerClosingForm form,
        boolean manual,
        String reason
    ) {
        var payload = mapper.createObjectNode()
            .put("manualAdjustment", manual)
            .put("confirmWarnings", true);
        if (manual) {
            payload.put("initialOdometer", form.getInitialOdometer().toPlainString())
                .put("finalOdometer", form.getFinalOdometer().toPlainString())
                .put("professionalKilometers", form.getProfessionalKilometers().toPlainString())
                .put("adjustmentReason", reason);
        }
        drafts.save("closing-submission-owner", new SaveDraftCommand(
            FormDraftType.MILEAGE_CLOSING,
            form.draftContextKey(),
            1,
            manual ? 3 : 2,
            null,
            payload,
            false
        ));
    }

    private Scenario closedScenario() {
        var vehicle = createVehicle();
        YearMonth month = YearMonth.now().plusMonths(2);
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
        String plate = "M" + UUID.randomUUID().toString().replace("-", "")
            .substring(0, 6).toUpperCase();
        return vehicles.create(new VehicleService.CreateVehicleCommand(
            "Veículo fechamento", "Toyota", "Etios", 2018, plate,
            FuelType.FLEX, new BigDecimal("10000.0"), new BigDecimal("35000.00")
        ));
    }

    private record Scenario(UUID vehicleId, YearMonth month) {
    }
}
