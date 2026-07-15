package dev.harrison.rendacomcarro.finance.application;

public class ObligationCalculationException extends IllegalArgumentException {
    private final String field;

    public ObligationCalculationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
