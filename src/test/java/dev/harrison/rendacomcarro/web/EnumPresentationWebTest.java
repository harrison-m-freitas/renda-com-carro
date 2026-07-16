package dev.harrison.rendacomcarro.web;

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

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=enum-presentation-owner",
    "APP_ADMIN_PASSWORD=enum-presentation-owner-credential"
})
class EnumPresentationWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void vehicleFormUsesFriendlyFuelLabels() throws Exception {
        mvc.perform(get("/vehicles/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(">Gasolina</option>")))
            .andExpect(content().string(not(containsString(">GASOLINE</option>"))));
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void expenseFormUsesFriendlyClassificationAndAllocationLabels() throws Exception {
        mvc.perform(get("/expenses/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<span>Profissional</span>")))
            .andExpect(content().string(containsString("value=\"PROFESSIONAL\"")))
            .andExpect(content().string(containsString("<span>Quilometragem</span>")))
            .andExpect(content().string(containsString("value=\"MILEAGE_RATIO\"")))
            .andExpect(content().string(not(containsString(">PROFESSIONAL</option>"))))
            .andExpect(content().string(not(containsString(">MILEAGE_RATIO</option>"))));
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void fuelingFormUsesFriendlyFuelLabels() throws Exception {
        mvc.perform(get("/fuelings/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(">Etanol</option>")))
            .andExpect(content().string(not(containsString(">ETHANOL</option>"))));
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void obligationFormUsesFriendlyTypeAndModeLabels() throws Exception {
        mvc.perform(get("/obligations/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Empréstimo de uma pessoa")))
            .andExpect(content().string(containsString("Parcelas fixas")))
            .andExpect(content().string(containsString("Pagamentos livres")))
            .andExpect(content().string(not(containsString(">STRUCTURED</option>"))));
    }
}