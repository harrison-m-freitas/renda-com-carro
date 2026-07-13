package dev.harrison.rendacomcarro.expense.application;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum OdometerOrigin implements LabeledEnum {
    PREVIOUS_MONTH_CLOSING("Fechamento do mês anterior"),
    FIRST_OPERATIONAL_DAY("Primeiro dia operacional do mês"),
    FIRST_SHIFT("Primeiro turno do mês"),
    FIRST_FUELING("Primeiro abastecimento do mês"),
    VEHICLE_INITIAL("Odômetro inicial do veículo"),
    CLOSED_OPERATIONAL_DAY("Último dia operacional fechado"),
    CLOSED_SHIFT("Último turno fechado"),
    FUELING("Último abastecimento"),
    CURRENT_VEHICLE("Odômetro atual do veículo");

    private final String label;

    OdometerOrigin(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
