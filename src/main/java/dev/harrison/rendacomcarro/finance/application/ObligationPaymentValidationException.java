package dev.harrison.rendacomcarro.finance.application;

public class ObligationPaymentValidationException extends IllegalArgumentException {
    private final String field;

    public ObligationPaymentValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
