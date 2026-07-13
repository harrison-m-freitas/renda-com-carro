package dev.harrison.rendacomcarro.vehicle;

import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=vehicle-web-owner",
    "APP_ADMIN_PASSWORD=vehicle-web-owner-credential"
})
class VehicleWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void vehicleListRenders() throws Exception {
        mvc.perform(get("/vehicles"))
            .andExpect(status().isOk())
            .andExpect(view().name("vehicles/list"));
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void vehicleFormRendersSemanticSectionsAndContextualActions() throws Exception {
        mvc.perform(get("/vehicles/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("vehicles/form"))
            .andExpect(content().string(containsString("Identificação")))
            .andExpect(content().string(containsString("Dados de operação")))
            .andExpect(content().string(containsString("Aquisição")))
            .andExpect(content().string(containsString("Cadastrar veículo")))
            .andExpect(content().string(containsString("Apelido do veículo")));
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void validVehicleCanBeCreated() throws Exception {
        mvc.perform(post("/vehicles")
                .with(csrf())
                .param("name", "Etios")
                .param("make", "Toyota")
                .param("model", "Etios")
                .param("year", "2015")
                .param("plate", "ABC1D23")
                .param("fuelType", "FLEX")
                .param("initialOdometer", "0.0")
                .param("purchasePrice", "30000.00"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/vehicles"));
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void blankNicknameAndFormattedLegacyPlateAreNormalized() throws Exception {
        mvc.perform(post("/vehicles")
                .with(csrf())
                .param("name", "")
                .param("make", "Renault")
                .param("model", "Sandero")
                .param("year", "2013")
                .param("plate", "abc-1234")
                .param("fuelType", "FLEX")
                .param("initialOdometer", "248351.0")
                .param("purchasePrice", "23990.00"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/vehicles"));
    }
}
