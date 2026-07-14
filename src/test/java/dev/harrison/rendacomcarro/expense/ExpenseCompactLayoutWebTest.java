package dev.harrison.rendacomcarro.expense;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    "APP_ADMIN_USERNAME=expense-compact-owner",
    "APP_ADMIN_PASSWORD=expense-compact-owner-credential"
})
@Transactional
class ExpenseCompactLayoutWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "expense-compact-owner", roles = "OWNER")
    void newExpenseUsesCompactAccessibleChoiceGroups() throws Exception {
        mvc.perform(get("/expenses/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("expense-segmented-group")))
            .andExpect(content().string(containsString("expense-allocation-methods")))
            .andExpect(content().string(containsString("data-classification-description")))
            .andExpect(content().string(containsString("data-allocation-method-description")))
            .andExpect(content().string(containsString("data-payment-description")))
            .andExpect(content().string(containsString("expense-reference-control")))
            .andExpect(content().string(not(containsString("classification-card-grid"))))
            .andExpect(content().string(not(containsString("classification-card__content"))))
            .andExpect(content().string(not(containsString("payment-status-choice"))))
            .andExpect(content().string(not(containsString("allocation-choice-list"))));
    }
}