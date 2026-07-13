package dev.harrison.rendacomcarro.goal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
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
    "APP_ADMIN_PASSWORD=goal-web-owner-password"
})
@Transactional
class GoalWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "goal-web-owner", roles = "OWNER")
    void goalFormUsesGuidedLocalizedInputsAndMonthDraft() throws Exception {
        mvc.perform(get("/goals/new").param("month", "2027-03"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-guided-form")))
            .andExpect(content().string(containsString("data-draft-type=\"MONTHLY_GOAL\"")))
            .andExpect(content().string(containsString("data-draft-context-key=\"month:2027-03\"")))
            .andExpect(content().string(containsString("data-form-step=\"1\"")))
            .andExpect(content().string(containsString("data-form-step=\"2\"")))
            .andExpect(content().string(containsString("data-form-step=\"3\"")))
            .andExpect(content().string(containsString("R$")))
            .andExpect(content().string(containsString("type=\"month\"")))
            .andExpect(content().string(containsString("Dias planejados")))
            .andExpect(content().string(containsString("type=\"module\"")));
    }

    @Test
    @WithMockUser(username = "goal-web-owner", roles = "OWNER")
    void postsBrazilianValuesAndPlannedDates() throws Exception {
        mvc.perform(post("/goals")
                .with(csrf())
                .param("month", "2027-04")
                .param("personalNetGoal", "2.500,00")
                .param("operationalGoal", "4.000,00")
                .param("plannedHours", "160")
                .param("plannedDates", "2027-04-01, 2027-04-02"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/goals"));
    }

    @Test
    @WithMockUser(username = "goal-web-owner", roles = "OWNER")
    void invalidSundayReturnsGuidedFormWithFriendlyError() throws Exception {
        mvc.perform(post("/goals")
                .with(csrf())
                .param("month", "2027-08")
                .param("personalNetGoal", "2500")
                .param("operationalGoal", "4000")
                .param("plannedHours", "160")
                .param("plannedDates", "2027-08-01"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Domingos")))
            .andExpect(content().string(containsString("data-guided-form")));
    }
}
