package dev.harrison.rendacomcarro.draft.application;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;

public interface FormDraftDefinition {
    FormDraftType type();

    int schemaVersion();

    int maxStep();

    String normalizeContextKey(String contextKey);

    ObjectNode normalizeAndValidate(ObjectNode payload, int currentStep);
}
