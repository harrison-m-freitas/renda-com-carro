package dev.harrison.rendacomcarro.draft;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.application.FormDraftService.SaveDraftCommand;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.security.domain.AppUser;
import dev.harrison.rendacomcarro.security.infrastructure.AppUserRepository;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=draft-api-owner",
    "APP_ADMIN_PASSWORD=draft-api-owner-password"
})
class FormDraftApiTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired FormDraftService drafts;
    @Autowired AppUserRepository users;

    @BeforeEach
    void ensureOtherOwner() {
        if (users.findByUsername("other-api-owner").isEmpty()) {
            users.save(new AppUser("other-api-owner", "unused-api-password-hash"));
        }
    }

    @Test
    void savesLoadsAndDeletesOwnerDraft() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        mvc.perform(put("/api/form-drafts/EXPENSE")
                .with(csrf())
                .with(user("draft-api-owner").roles("OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "contextKey":"current",
                      "schemaVersion":1,
                      "currentStep":1,
                      "version":null,
                      "force":false,
                      "payload":{
                        "vehicleId":"%s",
                        "categoryId":"%s",
                        "expenseDate":"2026-07-13",
                        "amount":"100,00"
                      }
                    }
                    """.formatted(vehicleId, categoryId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contextKey").value("current"))
            .andExpect(jsonPath("$.version").value(0));

        mvc.perform(get("/api/form-drafts/EXPENSE")
                .with(user("draft-api-owner").roles("OWNER"))
                .param("contextKey", "current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payload.amount").value("100.00"));

        mvc.perform(delete("/api/form-drafts/EXPENSE")
                .with(csrf())
                .with(user("draft-api-owner").roles("OWNER"))
                .param("contextKey", "current")
                .param("version", "0"))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/form-drafts/EXPENSE")
                .with(user("draft-api-owner").roles("OWNER"))
                .param("contextKey", "current"))
            .andExpect(status().isNotFound());
    }

    @Test
    void apiRequiresAuthenticationAndCsrf() throws Exception {
        mvc.perform(get("/api/form-drafts/EXPENSE").param("contextKey", "current"))
            .andExpect(status().is3xxRedirection());

        mvc.perform(put("/api/form-drafts/EXPENSE")
                .with(user("draft-api-owner").roles("OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void validationAndConflictResponsesAreJson() throws Exception {
        String initial = requestBody(validExpensePayload("100,00"), null, false);
        mvc.perform(put("/api/form-drafts/EXPENSE")
                .with(csrf())
                .with(user("draft-api-owner").roles("OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(initial))
            .andExpect(status().isOk());

        ObjectNode unknown = validExpensePayload("110,00");
        unknown.put("calculatedTotal", "110,00");
        mvc.perform(put("/api/form-drafts/EXPENSE")
                .with(csrf())
                .with(user("draft-api-owner").roles("OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(unknown, 0L, false)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                org.hamcrest.Matchers.containsString("Campo de rascunho não permitido")
            ));

        mvc.perform(put("/api/form-drafts/EXPENSE")
                .with(csrf())
                .with(user("draft-api-owner").roles("OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(validExpensePayload("120,00"), 0L, false)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(1));

        mvc.perform(put("/api/form-drafts/EXPENSE")
                .with(csrf())
                .with(user("draft-api-owner").roles("OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(validExpensePayload("130,00"), 0L, false)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.current.version").value(1));
    }

    @Test
    void obligationListDoesNotExposeAnotherOwnerDraft() throws Exception {
        drafts.save("draft-api-owner", obligationCommand("draft:" + UUID.randomUUID()));
        drafts.save("other-api-owner", obligationCommand("draft:" + UUID.randomUUID()));

        mvc.perform(get("/api/form-drafts/OBLIGATION/list")
                .with(user("draft-api-owner").roles("OWNER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    private ObjectNode validExpensePayload(String amount) {
        return mapper.createObjectNode()
            .put("vehicleId", UUID.randomUUID().toString())
            .put("categoryId", UUID.randomUUID().toString())
            .put("expenseDate", "2026-07-13")
            .put("amount", amount);
    }

    private String requestBody(ObjectNode payload, Long version, boolean force) throws Exception {
        ObjectNode request = mapper.createObjectNode()
            .put("contextKey", "current")
            .put("schemaVersion", 1)
            .put("currentStep", 1)
            .put("force", force)
            .set("payload", payload);
        if (version == null) {
            request.putNull("version");
        } else {
            request.put("version", version);
        }
        return mapper.writeValueAsString(request);
    }

    private SaveDraftCommand obligationCommand(String key) {
        return new SaveDraftCommand(
            FormDraftType.OBLIGATION,
            key,
            1,
            1,
            null,
            mapper.createObjectNode()
                .put("creditor", "Credor")
                .put("type", "FAMILY_LOAN"),
            false
        );
    }
}
