package dev.harrison.rendacomcarro.finance.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum ObligationMode implements LabeledEnum {
    FIXED_INSTALLMENTS("Parcelas fixas"),
    FLEXIBLE_PAYMENTS("Pagamentos livres"),
    SINGLE_PAYMENT("Pagamento único");

    private final String label;

    ObligationMode(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
