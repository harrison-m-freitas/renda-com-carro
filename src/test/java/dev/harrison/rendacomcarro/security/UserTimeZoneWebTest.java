package dev.harrison.rendacomcarro.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.harrison.rendacomcarro.security.application.UserTimeZoneService;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=time-zone-owner",
    "APP_ADMIN_PASSWORD=time-zone-owner-password"
})
@Transactional
class UserTimeZoneWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserTimeZoneService timeZones;

    @Test
    @WithMockUser(username = "time-zone-owner", roles = "OWNER")
    void authenticatedLayoutExposesSavedAndActiveZones() throws Exception {
        timeZones.update("time-zone-owner", "America/Sao_Paulo");

        mvc.perform(get("/expenses/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-saved-time-zone=\"America/Sao_Paulo\"")))
            .andExpect(content().string(containsString("data-active-time-zone=\"America/Sao_Paulo\"")))
            .andExpect(content().string(containsString("Usar fuso deste dispositivo")))
            .andExpect(content().string(containsString("Manter fuso configurado")));
    }

    @Test
    @WithMockUser(username = "time-zone-owner", roles = "OWNER")
    void preferenceCanBeReadAndUpdatedOnlyWithExplicitRequest() throws Exception {
        mvc.perform(put("/api/user-preferences/time-zone")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeZoneId\":\"Pacific/Kiritimati\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.savedTimeZoneId").value("Pacific/Kiritimati"))
            .andExpect(jsonPath("$.activeTimeZoneId").value("Pacific/Kiritimati"));

        mvc.perform(get("/api/user-preferences/time-zone"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.savedTimeZoneId").value("Pacific/Kiritimati"));
    }

    @Test
    @WithMockUser(username = "time-zone-owner", roles = "OWNER")
    void invalidPreferenceReturnsBadRequest() throws Exception {
        mvc.perform(put("/api/user-preferences/time-zone")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeZoneId\":\"Mars/Olympus\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Fuso horário inválido."));
    }
}
