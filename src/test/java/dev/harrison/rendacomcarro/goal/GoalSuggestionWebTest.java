package dev.harrison.rendacomcarro.goal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.harrison.rendacomcarro.goal.application.OperationalGoalSuggestion;
import dev.harrison.rendacomcarro.goal.application.OperationalGoalSuggestionService;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=goal-suggestion-owner",
    "APP_ADMIN_PASSWORD=goal-suggestion-password"
})
class GoalSuggestionWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;

    @MockitoBean
    OperationalGoalSuggestionService suggestions;

    @Test
    @WithMockUser(username = "goal-suggestion-owner", roles = "OWNER")
    void returnsLocalizedTransparentSuggestionAsDecimalStrings() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        when(suggestions.suggest(YearMonth.of(2026, 7), Set.of(vehicleId)))
            .thenReturn(new OperationalGoalSuggestion(
                YearMonth.of(2026, 7),
                "Julho de 2026",
                Set.of(vehicleId),
                new BigDecimal("930.00"),
                new BigDecimal("280.00"),
                new BigDecimal("1200.00"),
                new BigDecimal("320.00"),
                new BigDecimal("2730.00"),
                List.of(),
                List.of(),
                List.of()
            ));

        mvc.perform(get("/goals/operational-suggestion")
                .param("month", "2026-07")
                .param("vehicleIds", vehicleId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.month").value("2026-07"))
            .andExpect(jsonPath("$.monthLabel").value("Julho de 2026"))
            .andExpect(jsonPath("$.professionalCostsTotal").value("2730.00"))
            .andExpect(jsonPath("$.currentExpenses").value("930.00"));
    }

    @Test
    @WithMockUser(username = "goal-suggestion-owner", roles = "OWNER")
    void requiresAtLeastOneVehicle() throws Exception {
        mvc.perform(get("/goals/operational-suggestion")
                .param("month", "2026-07"))
            .andExpect(status().isBadRequest());
    }
}
