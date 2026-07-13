package dev.harrison.rendacomcarro.vehicle;

import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import dev.harrison.rendacomcarro.vehicle.infrastructure.VehicleRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
    @Autowired VehicleService vehicleService;
    @Autowired VehicleRepository vehicleRepository;

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void vehicleListRenders() throws Exception {
        mvc.perform(get("/vehicles"))
            .andExpect(status().isOk())
            .andExpect(view().name("vehicles/list"));
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void newVehicleFormUsesGroupedResponsiveLayout() throws Exception {
        mvc.perform(get("/vehicles/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("vehicles/form"))
            .andExpect(content().string(containsString("Identificação")))
            .andExpect(content().string(containsString("Dados de operação")))
            .andExpect(content().string(containsString("Aquisição")))
            .andExpect(content().string(containsString("Apelido do veículo")))
            .andExpect(content().string(containsString("Cadastrar veículo")))
            .andExpect(content().string(containsString("appMobileNavigation")))
            .andExpect(content().string(containsString("vehicle-identification-grid")))
            .andExpect(content().string(containsString("vehicle-operation-grid")))
            .andExpect(content().string(containsString("vehicle-acquisition-grid")))
            .andExpect(content().string(containsString("data-normalize-spaces")))
            .andExpect(content().string(containsString("data-odometer-input")))
            .andExpect(content().string(containsString("data-money-input")))
            .andExpect(content().string(containsString("data-max-digits=\"14\"")))
            .andExpect(content().string(containsString("data-max-integer-digits=\"11\"")))
            .andExpect(content().string(containsString("type=\"module\"")))
            .andExpect(content().string(not(containsString(">Salvar veículo<"))));
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void blankNicknameLegacyPlateAndBlankPriceArePreservedSemantically() throws Exception {
        mvc.perform(post("/vehicles")
                .with(csrf())
                .param("name", "")
                .param("make", "Toyota")
                .param("model", "Etios")
                .param("year", "2015")
                .param("plate", "xyz-9876")
                .param("fuelType", "FLEX")
                .param("initialOdometer", "248.351")
                .param("purchasePrice", ""))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/vehicles"));

        var vehicle = vehicleRepository.findAll().stream()
            .filter(candidate -> candidate.getPlate().equals("XYZ9876"))
            .findFirst()
            .orElseThrow();
        assertThat(vehicle.getName()).isEqualTo("Toyota Etios");
        assertThat(vehicle.getCurrentOdometer()).isEqualByComparingTo("248351.0");
        assertThat(vehicle.getPurchasePrice()).isNull();

        mvc.perform(get("/vehicles/{id}", vehicle.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Não informado")))
            .andExpect(content().string(not(containsString("R$ 0,00"))));
    }

    @Test
    @WithMockUser(username = "harrison", roles = "OWNER")
    void editVehicleUsesSpecificActionAndTechnicalSummary() throws Exception {
        var vehicle = vehicleService.create(new VehicleService.CreateVehicleCommand(
            "Sandero principal", "Renault", "Sandero", 2013, "QWE1R23", FuelType.FLEX,
            new BigDecimal("120000"), new BigDecimal("23990")));

        mvc.perform(get("/vehicles/{id}/edit", vehicle.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Salvar alterações")))
            .andExpect(content().string(containsString("Renault Sandero")))
            .andExpect(content().string(containsString("QWE1R23")));
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
                .param("initialOdometer", "0,0")
                .param("purchasePrice", "30.000,00"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/vehicles"));
    }
}
