package dev.harrison.rendacomcarro.draft.application;

import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class FormDraftDefinitionRegistry {
    private final Map<FormDraftType, FormDraftDefinition> definitions;

    public FormDraftDefinitionRegistry(List<FormDraftDefinition> definitions) {
        this.definitions = definitions.stream().collect(Collectors.toUnmodifiableMap(
            FormDraftDefinition::type,
            Function.identity()
        ));
    }

    public FormDraftDefinition require(FormDraftType type) {
        FormDraftDefinition definition = definitions.get(type);
        if (definition == null) {
            throw new DomainValidationException("Tipo de rascunho não suportado.");
        }
        return definition;
    }
}
