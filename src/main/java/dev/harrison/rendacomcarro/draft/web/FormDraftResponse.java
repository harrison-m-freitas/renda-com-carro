package dev.harrison.rendacomcarro.draft.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.application.FormDraftService.DraftView;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import java.time.LocalDateTime;
import java.util.UUID;

public record FormDraftResponse(
    UUID id,
    FormDraftType formType,
    String contextKey,
    int schemaVersion,
    int currentStep,
    ObjectNode payload,
    long version,
    LocalDateTime updatedAt,
    LocalDateTime expiresAt
) {
    public static FormDraftResponse from(DraftView view) {
        return new FormDraftResponse(
            view.id(),
            view.formType(),
            view.contextKey(),
            view.schemaVersion(),
            view.currentStep(),
            view.payload(),
            view.version(),
            view.updatedAt(),
            view.expiresAt()
        );
    }
}
