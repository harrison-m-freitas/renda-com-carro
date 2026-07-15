package dev.harrison.rendacomcarro.goal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.harrison.rendacomcarro.goal.application.GoalService;
import dev.harrison.rendacomcarro.goal.domain.WorkloadPeriodicity;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=goal-edit-owner",
    "APP_ADMIN_PASSWORD=goal-edit-owner-password"
})
@Transactional
class GoalEditEntryPointWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired GoalService goals;
    @Autowired VehicleService vehicles;

    @Test
    @WithMockUser(username = "goal-edit-owner", roles = "OWNER")
    void goalDetailExposesTheExistingEditFlow() throws Exception {
        YearMonth month = YearMonth.now();
        LocalDate plannedDate = firstAllowedDate(month);
        var vehicle = createVehicle();
        var goal = goals.create(
            month,
            new BigDecimal("2500.00"),
            new BigDecimal("4000.00"),
            WorkloadPeriodicity.MONTHLY,
            60,
            Set.of(plannedDate)
        );

        mvc.perform(get("/goals"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Editar meta")))
            .andExpect(content().string(containsString(
                "href=\"/goals/" + goal.getId() + "/edit\""
            )));
    }

    private dev.harrison.rendacomcarro.vehicle.domain.Vehicle createVehicle() {
        String plate = "G" + UUID.randomUUID().toString().replace("-", "")
            .substring(0, 6).toUpperCase();
        return vehicles.create(new VehicleService.CreateVehicleCommand(
            "Veículo edição da meta", "Toyota", "Etios", 2018, plate,
            FuelType.FLEX, new BigDecimal("10000.0"), new BigDecimal("35000.00")
        ));
    }

    private LocalDate firstAllowedDate(YearMonth month) {
        LocalDate date = month.atDay(1);
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}
