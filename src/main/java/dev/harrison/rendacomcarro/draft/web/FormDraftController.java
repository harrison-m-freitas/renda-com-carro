package dev.harrison.rendacomcarro.draft.web;

import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/form-drafts")
public class FormDraftController {
    private final FormDraftService drafts;

    public FormDraftController(FormDraftService drafts) {
        this.drafts = drafts;
    }

    @GetMapping("/OBLIGATION/list")
    public List<FormDraftSummaryResponse> listObligations(Authentication authentication) {
        return drafts.listActive(authentication.getName(), FormDraftType.OBLIGATION)
            .stream()
            .map(FormDraftSummaryResponse::from)
            .toList();
    }

    @GetMapping("/{type}")
    public ResponseEntity<FormDraftResponse> get(
        @PathVariable FormDraftType type,
        @RequestParam String contextKey,
        Authentication authentication
    ) {
        return drafts.find(authentication.getName(), type, contextKey)
            .map(FormDraftResponse::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{type}")
    public FormDraftResponse save(
        @PathVariable FormDraftType type,
        @RequestBody SaveFormDraftRequest request,
        Authentication authentication
    ) {
        return FormDraftResponse.from(drafts.save(
            authentication.getName(),
            request.toCommand(type)
        ));
    }

    @DeleteMapping("/{type}")
    public ResponseEntity<Void> discard(
        @PathVariable FormDraftType type,
        @RequestParam String contextKey,
        @RequestParam(required = false) Long version,
        Authentication authentication
    ) {
        drafts.discard(authentication.getName(), type, contextKey, version);
        return ResponseEntity.noContent().build();
    }
}
