package dev.harrison.rendacomcarro.expense;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
    "APP_ADMIN_USERNAME=expense-error-owner",
    "APP_ADMIN_PASSWORD=expense-error-owner-credential"
})
@Transactional
class ExpenseErrorSummaryWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "expense-error-owner", roles = "OWNER")
    void invalidSubmissionShowsCountedErrorSummaryWithFieldLinks() throws Exception {
        mvc.perform(post("/expenses")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("expenses/form"))
            .andExpect(content().string(containsString("data-expense-error-count")))
            .andExpect(content().string(containsString("href=\"#vehicleId\"")))
            .andExpect(content().string(containsString("href=\"#categoryId\"")))
            .andExpect(content().string(containsString("href=\"#amount\"")));
    }
}
