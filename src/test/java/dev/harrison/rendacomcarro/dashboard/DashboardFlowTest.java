package dev.harrison.rendacomcarro.dashboard;

import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=dashboard-owner",
    "APP_ADMIN_PASSWORD=dashboard-owner-credential"
})
class DashboardFlowTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void dashboardRendersOperationalHeading() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard/index"))
            .andExpect(content().string(containsString("Operação de hoje")));
    }
}
