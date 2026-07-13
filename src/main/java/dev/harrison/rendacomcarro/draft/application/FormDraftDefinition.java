package dev.harrison.rendacomcarro.draft.application;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;

public interface FormDraftDefinition {
    FormDraftType type();

    int schemaVersion();

    int maxStep();

    String normalizeContextKey(String contextKey);

    default ObjectNode normalizeAndValidate(ObjectNode payload, int currentStep) {
        return normalizeAndValidate(payload, currentStep, true);
    }

    ObjectNode normalizeAndValidate(
        ObjectNode payload,
        int currentStep,
        boolean validateCurrentStep
    );
}
