package dev.harrison.rendacomcarro.draft.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.application.FormDraftService.SaveDraftCommand;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;

public record SaveFormDraftRequest(
    String contextKey,
    int schemaVersion,
    int currentStep,
    Long version,
    boolean force,
    ObjectNode payload
) {
    public SaveDraftCommand toCommand(FormDraftType type) {
        return new SaveDraftCommand(
            type,
            contextKey,
            schemaVersion,
            currentStep,
            version,
            payload,
            force
        );
    }
}
