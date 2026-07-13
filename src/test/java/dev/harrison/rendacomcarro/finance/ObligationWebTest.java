package dev.harrison.rendacomcarro.finance;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.application.FormDraftService.SaveDraftCommand;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
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
    "APP_ADMIN_USERNAME=obligation-web-owner",
    "APP_ADMIN_PASSWORD=obligation-web-owner-password"
})
@Transactional
class ObligationWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired FormDraftService drafts;
    @Autowired ObjectMapper mapper;

    @Test
    @WithMockUser(username = "obligation-web-owner", roles = "OWNER")
    void obligationFormUsesGuidedLocalizedInputsAndUniqueDraftKey() throws Exception {
        String key = "draft:" + UUID.randomUUID();

        mvc.perform(get("/obligations/new").param("draftKey", key))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-guided-form")))
            .andExpect(content().string(containsString("data-draft-type=\"OBLIGATION\"")))
            .andExpect(content().string(containsString("data-draft-context-key=\"" + key + "\"")))
            .andExpect(content().string(containsString("data-form-step=\"1\"")))
            .andExpect(content().string(containsString("data-form-step=\"2\"")))
            .andExpect(content().string(containsString("data-form-step=\"3\"")))
            .andExpect(content().string(containsString("data-form-step=\"4\"")))
            .andExpect(content().string(containsString("R$")))
            .andExpect(content().string(containsString("Juros anuais")))
            .andExpect(content().string(containsString("%")))
            .andExpect(content().string(containsString("type=\"module\"")));
    }

    @Test
    @WithMockUser(username = "obligation-web-owner", roles = "OWNER")
    void postsFlexibleObligationUsingBrazilianValues() throws Exception {
        mvc.perform(post("/obligations")
                .with(csrf())
                .param("draftKey", "draft:" + UUID.randomUUID())
                .param("type", "FAMILY_LOAN")
                .param("mode", "FLEXIBLE")
                .param("creditor", "Família")
                .param("principal", "30.000,00")
                .param("annualRatePercent", "12")
                .param("startDate", "2026-07-13")
                .param("monthlyTarget", "500,00"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/obligations/*"));
    }

    @Test
    @WithMockUser(username = "obligation-web-owner", roles = "OWNER")
    void obligationListShowsOnlyOwnerDrafts() throws Exception {
        String ownKey = "draft:" + UUID.randomUUID();
        drafts.save("obligation-web-owner", new SaveDraftCommand(
            FormDraftType.OBLIGATION,
            ownKey,
            1,
            1,
            null,
            mapper.createObjectNode()
                .put("creditor", "Banco em rascunho")
                .put("type", "BANK_FINANCING"),
            false
        ));

        mvc.perform(get("/obligations")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(
                    "obligation-web-owner"
                ).roles("OWNER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Rascunhos em andamento")))
            .andExpect(content().string(containsString("Banco em rascunho")))
            .andExpect(content().string(containsString(ownKey)));
    }
}
