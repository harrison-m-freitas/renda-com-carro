package dev.harrison.rendacomcarro.draft;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=draft-autosave-owner",
    "APP_ADMIN_PASSWORD=draft-autosave-password"
})
@Transactional
class FormDraftAutosaveApiTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void partialAutosaveSucceedsButContinueRequiresCurrentStepFields() throws Exception {
        mvc.perform(put("/api/form-drafts/EXPENSE")
                .with(csrf())
                .with(user("draft-autosave-owner").roles("OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "contextKey":"current",
                      "schemaVersion":1,
                      "currentStep":1,
                      "version":null,
                      "validateCurrentStep":false,
                      "force":false,
                      "payload":{"amount":"120,50"}
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payload.amount").value("120.50"))
            .andExpect(jsonPath("$.version").value(0));

        mvc.perform(put("/api/form-drafts/EXPENSE")
                .with(csrf())
                .with(user("draft-autosave-owner").roles("OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "contextKey":"current",
                      "schemaVersion":1,
                      "currentStep":1,
                      "version":0,
                      "validateCurrentStep":true,
                      "force":false,
                      "payload":{"amount":"120,50"}
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                org.hamcrest.Matchers.containsString("Veículo")
            ));
    }
}
