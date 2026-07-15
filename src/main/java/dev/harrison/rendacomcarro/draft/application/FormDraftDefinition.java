package dev.harrison.rendacomcarro.draft.application;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;

public interface FormDraftDefinition {
    FormDraftType type();

    int schemaVersion();

    int maxStep();

    String normalizeContextKey(String contextKey);

    default ObjectNode migrate(int sourceSchemaVersion, ObjectNode payload) {
        if (sourceSchemaVersion != schemaVersion()) {
            throw new DomainValidationException("Versão de rascunho incompatível.");
        }
        return payload.deepCopy();
    }

    default ObjectNode normalizeAndValidate(ObjectNode payload, int currentStep) {
        return normalizeAndValidate(payload, currentStep, true);
    }

    ObjectNode normalizeAndValidate(
        ObjectNode payload,
        int currentStep,
        boolean validateCurrentStep
    );
}
