package dev.harrison.rendacomcarro.goal;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=goal-web-owner",
    "APP_ADMIN_PASSWORD=goal-web-password"
})
@Transactional
class GoalWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired GoalService goals;
    @Autowired VehicleService vehicles;

    private Vehicle activeVehicle;

    @BeforeEach
    void createActiveVehicle() {
        String plate = "G" + UUID.randomUUID().toString().replace("-", "")
            .substring(0, 6).toUpperCase();
        activeVehicle = vehicles.create(new VehicleService.CreateVehicleCommand(
            "Veículo da meta web", "Toyota", "Etios", 2018, plate, FuelType.FLEX,
            new BigDecimal("10000.0"), new BigDecimal("35000.00")
        ));
    }

    @Test
    @WithMockUser(username = "goal-web-owner", roles = "OWNER")
    void goalFormUsesGuidedLocalizedInputsAndWorkloadSourceFields() throws Exception {
        mvc.perform(get("/goals/new").param("month", "2027-03"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-guided-form")))
            .andExpect(content().string(containsString("data-draft-type=\"MONTHLY_GOAL\"")))
            .andExpect(content().string(containsString("data-draft-context-key=\"month:2027-03\"")))
            .andExpect(content().string(containsString("data-draft-schema-version=\"4\"")))
            .andExpect(content().string(containsString("data-goal-month-picker")))
            .andExpect(content().string(containsString("aria-haspopup=\"dialog\"")))
            .andExpect(content().string(containsString("data-goal-calendar")))
            .andExpect(content().string(containsString("data-goal-financial-breakdown")))
            .andExpect(content().string(containsString("data-goal-rate-operational-day")))
            .andExpect(content().string(containsString(activeVehicle.getName())))
            .andExpect(content().string(containsString("name=\"workloadPeriodicity\"")))
            .andExpect(content().string(containsString("id=\"workload-DAILY\"")))
            .andExpect(content().string(containsString("for=\"workload-DAILY\"")))
            .andExpect(content().string(containsString("id=\"workload-WEEKLY\"")))
            .andExpect(content().string(containsString("for=\"workload-WEEKLY\"")))
            .andExpect(content().string(containsString("id=\"workload-MONTHLY\"")))
            .andExpect(content().string(containsString("for=\"workload-MONTHLY\"")))
            .andExpect(content().string(containsString("name=\"workloadHours\"")))
            .andExpect(content().string(containsString("name=\"workloadMinutes\"")))
            .andExpect(content().string(containsString("Por dia")))
            .andExpect(content().string(containsString("Por semana")))
            .andExpect(content().string(containsString("Por mês")))
            .andExpect(content().string(containsString("data-workload-summary")))
            .andExpect(content().string(not(containsString("name=\"plannedHours\""))))
            .andExpect(content().string(containsString("data-money-input")))
            .andExpect(content().string(containsString("Dias planejados")))
            .andExpect(content().string(containsString("type=\"module\"")));
    }

    @Test
    @WithMockUser(username = "goal-web-owner", roles = "OWNER")
    void editFormRestoresOriginalWeeklySourceAndUpdatesAuthoritatively() throws Exception {
        YearMonth month = YearMonth.of(2027, 5);
        var goal = goals.create(
            month,
            new BigDecimal("2500.00"),
            new BigDecimal("4000.00"),
            WorkloadPeriodicity.WEEKLY,
            2_430,
            Set.of(
                LocalDate.of(2027, 5, 3),
                LocalDate.of(2027, 5, 4),
                LocalDate.of(2027, 5, 5),
                LocalDate.of(2027, 5, 6),
                LocalDate.of(2027, 5, 7)
            )
        );

        mvc.perform(get("/goals/{id}/edit", goal.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Editar meta mensal")))
            .andExpect(content().string(containsString("data-draft-recovery-mode=\"none\"")))
            .andExpect(content().string(containsString(
                "action=\"/goals/" + goal.getId() + "\""
            )))
            .andExpect(content().string(containsString("Atualizar meta")))
            .andExpect(model().attribute("goalForm", allOf(
                hasProperty("workloadPeriodicity", is(WorkloadPeriodicity.WEEKLY)),
                hasProperty("workloadHours", is(40L)),
                hasProperty("workloadMinutes", is(30))
            )));

        mvc.perform(post("/goals/{id}", goal.getId())
                .with(csrf())
                .param("month", month.toString())
                .param("personalNetGoal", "2.600,00")
                .param("operationalGoal", "4.100,00")
                .param("workloadPeriodicity", "DAILY")
                .param("workloadHours", "8")
                .param("workloadMinutes", "30")
                .param("plannedDates", "2027-05-03,2027-05-04"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/goals"));

        var updated = goals.find(month).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getWorkloadPeriodicity())
            .isEqualTo(WorkloadPeriodicity.DAILY);
        org.assertj.core.api.Assertions.assertThat(updated.getEnteredDurationMinutes())
            .isEqualTo(510);
        org.assertj.core.api.Assertions.assertThat(updated.getCalculatedMonthMinutes())
            .isEqualTo(1_020);
    }

    @Test
    @WithMockUser(username = "goal-web-owner", roles = "OWNER")
    void postsBrazilianValuesAndWeeklyWorkload() throws Exception {
        mvc.perform(post("/goals")
                .with(csrf())
                .param("month", "2027-04")
                .param("personalNetGoal", "2.500,00")
                .param("operationalGoal", "4.000,00")
                .param("workloadPeriodicity", "WEEKLY")
                .param("workloadHours", "40")
                .param("workloadMinutes", "0")
                .param("plannedDates", "2027-04-01, 2027-04-02"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/goals"));
    }

    @Test
    @WithMockUser(username = "goal-web-owner", roles = "OWNER")
    void ignoresManipulatedCalculatedWorkloadFieldsAndRecalculatesOnTheServer() throws Exception {
        YearMonth month = YearMonth.of(2027, 6);

        mvc.perform(post("/goals")
                .with(csrf())
                .param("month", month.toString())
                .param("personalNetGoal", "2.500,00")
                .param("operationalGoal", "4.000,00")
                .param("workloadPeriodicity", "WEEKLY")
                .param("workloadHours", "40")
                .param("workloadMinutes", "0")
                .param("plannedDates", "2027-06-01,2027-06-02")
                .param("plannedHours", "9999.99")
                .param("calculatedMonthMinutes", "999999")
                .param("allocatedDurationMinutes", "999999"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/goals"));

        var saved = goals.find(month).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(saved.getEnteredDurationMinutes())
            .isEqualTo(2_400);
        org.assertj.core.api.Assertions.assertThat(saved.getCalculatedMonthMinutes())
            .isEqualTo(960);
        org.assertj.core.api.Assertions.assertThat(saved.getPlannedHours())
            .isEqualByComparingTo("16.00");
        org.assertj.core.api.Assertions.assertThat(goals.plannedDays(saved.getId()))
            .extracting(day -> day.getAllocatedDurationMinutes())
            .containsExactly(480L, 480L);
    }

    @Test
    @WithMockUser(username = "goal-web-owner", roles = "OWNER")
    void invalidSundayReturnsGuidedFormWithFriendlyError() throws Exception {
        mvc.perform(post("/goals")
                .with(csrf())
                .param("month", "2027-08")
                .param("personalNetGoal", "2500")
                .param("operationalGoal", "4000")
                .param("workloadPeriodicity", "MONTHLY")
                .param("workloadHours", "160")
                .param("workloadMinutes", "0")
                .param("plannedDates", "2027-08-01"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Domingos")))
            .andExpect(content().string(containsString("data-guided-form")));
    }
}
