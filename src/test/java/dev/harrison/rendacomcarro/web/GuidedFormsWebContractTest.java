package dev.harrison.rendacomcarro.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;
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
    "APP_ADMIN_USERNAME=guided-contract-owner",
    "APP_ADMIN_PASSWORD=guided-contract-password"
})
@Transactional
class GuidedFormsWebContractTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired VehicleService vehicles;

    @Test
    @WithMockUser(username = "guided-contract-owner", roles = "OWNER")
    void complexFormsContainGuidedSectionsRecoveryConflictAndRealSubmitButtons() throws Exception {
        var vehicle = createVehicle();
        YearMonth month = YearMonth.of(2028, 1);

        assertGuided("/expenses/new", "EXPENSE", "Salvar gasto");
        assertGuided("/goals/new?month=2028-01", "MONTHLY_GOAL", "Salvar meta");
        assertGuided(
            "/obligations/new?draftKey=draft:" + UUID.randomUUID(),
            "OBLIGATION",
            "Salvar obrigação"
        );
        assertGuided(
            "/mileage-closings/new?vehicleId=" + vehicle.getId() + "&month=" + month,
            "MILEAGE_CLOSING",
            "Fechamento bloqueado"
        );
    }

    @Test
    @WithMockUser(username = "guided-contract-owner", roles = "OWNER")
    void simpleFormsRemainCompact() throws Exception {
        mvc.perform(get("/vehicles/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("data-guided-form"))));

        createVehicle();
        mvc.perform(get("/operation-days/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("data-guided-form"))));
    }

    private void assertGuided(String path, String type, String finalCopy) throws Exception {
        mvc.perform(get(path))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-guided-form")))
            .andExpect(content().string(containsString("data-draft-type=\"" + type + "\"")))
            .andExpect(content().string(containsString("data-guided-dialog=\"recovery\"")))
            .andExpect(content().string(containsString("data-guided-dialog=\"conflict\"")))
            .andExpect(content().string(containsString("data-guided-save-status")))
            .andExpect(content().string(containsString(finalCopy)));
    }

    private dev.harrison.rendacomcarro.vehicle.domain.Vehicle createVehicle() {
        String plate = "G" + UUID.randomUUID().toString().replace("-", "")
            .substring(0, 6).toUpperCase();
        return vehicles.create(new VehicleService.CreateVehicleCommand(
            "Veículo contrato", "Honda", "Fit", 2018, plate,
            FuelType.FLEX, new BigDecimal("10000.0"), new BigDecimal("35000.00")
        ));
    }
}
