package dev.harrison.rendacomcarro.expense;

import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties={"APP_ADMIN_USERNAME=expense-owner","APP_ADMIN_PASSWORD=expense-owner-credential"})
class ExpenseWebTest extends PostgresIntegrationTest {
 @Autowired MockMvc mvc;
 @Test @WithMockUser
 void expenseListIsAvailableToAuthenticatedOwner() throws Exception {
  mvc.perform(get("/expenses")).andExpect(status().isOk()).andExpect(view().name("expenses/list"));
 }
}
