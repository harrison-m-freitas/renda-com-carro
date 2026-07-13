package dev.harrison.rendacomcarro.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

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
            .andExpect(xpath("//select[@name='fuelType']/option[normalize-space(.)='Gasolina']").exists())
            .andExpect(xpath("//select[@name='fuelType']/option[normalize-space(.)='GASOLINE']").doesNotExist());
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void expenseFormUsesFriendlyClassificationAndAllocationLabels() throws Exception {
        mvc.perform(get("/expenses/new"))
            .andExpect(status().isOk())
            .andExpect(xpath("//select[@name='classification']/option[normalize-space(.)='Profissional']").exists())
            .andExpect(xpath("//select[@name='classification']/option[normalize-space(.)='PROFESSIONAL']").doesNotExist())
            .andExpect(xpath("//select[@name='allocationMethod']/option[normalize-space(.)='Proporcional à quilometragem']").exists())
            .andExpect(xpath("//select[@name='allocationMethod']/option[normalize-space(.)='MILEAGE_RATIO']").doesNotExist());
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void fuelingFormUsesFriendlyFuelLabels() throws Exception {
        mvc.perform(get("/fuelings/new"))
            .andExpect(status().isOk())
            .andExpect(xpath("//select[@name='fuelType']/option[normalize-space(.)='Etanol']").exists())
            .andExpect(xpath("//select[@name='fuelType']/option[normalize-space(.)='ETHANOL']").doesNotExist());
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void obligationFormUsesFriendlyTypeAndModeLabels() throws Exception {
        mvc.perform(get("/obligations/new"))
            .andExpect(status().isOk())
            .andExpect(xpath("//select[@name='type']/option[normalize-space(.)='Empréstimo familiar']").exists())
            .andExpect(xpath("//select[@name='type']/option[normalize-space(.)='FAMILY_LOAN']").doesNotExist())
            .andExpect(xpath("//select[@name='mode']/option[normalize-space(.)='Parcelas programadas']").exists())
            .andExpect(xpath("//select[@name='mode']/option[normalize-space(.)='STRUCTURED']").doesNotExist());
    }
}
