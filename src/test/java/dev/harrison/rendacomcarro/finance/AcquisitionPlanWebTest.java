package dev.harrison.rendacomcarro.finance;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
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
    "APP_ADMIN_USERNAME=acquisition-web-owner",
    "APP_ADMIN_PASSWORD=acquisition-web-password"
})
@Transactional
class AcquisitionPlanWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "acquisition-web-owner", roles = "OWNER")
    void formExplainsThatAPlanCanContainSeveralFundingSources() throws Exception {
        mvc.perform(get("/acquisition-plans/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Plano de compra")))
            .andExpect(content().string(containsString("várias fontes")))
            .andExpect(content().string(containsString("data-financial-money")));
    }

    @Test
    @WithMockUser(username = "acquisition-web-owner", roles = "OWNER")
    void createsPlanUsingBrazilianValues() throws Exception {
        mvc.perform(post("/acquisition-plans")
                .with(csrf())
                .param("title", "Compra do Renault")
                .param("purchaseAmount", "45.000,00")
                .param("ownResourcesAmount", "10.000,00")
                .param("purchaseDate", "2026-07-14"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/acquisition-plans/*"));
    }
}
