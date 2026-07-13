package dev.harrison.rendacomcarro.operation.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum DataSource implements LabeledEnum {
    MANUAL("Manual"),
    IMPORT("Importação"),
    COLLECTOR("Coletor"),
    API("API");

    private final String label;

    DataSource(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
