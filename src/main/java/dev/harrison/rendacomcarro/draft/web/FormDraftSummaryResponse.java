package dev.harrison.rendacomcarro.draft.web;

import dev.harrison.rendacomcarro.draft.application.FormDraftService.DraftView;
import java.time.LocalDateTime;
import java.util.UUID;

public record FormDraftSummaryResponse(
    UUID id,
    String contextKey,
    int currentStep,
    LocalDateTime updatedAt,
    LocalDateTime expiresAt
) {
    public static FormDraftSummaryResponse from(DraftView view) {
        return new FormDraftSummaryResponse(
            view.id(),
            view.contextKey(),
            view.currentStep(),
            view.updatedAt(),
            view.expiresAt()
        );
    }
}
