package dev.harrison.rendacomcarro.expense.application;

public class ExpenseFormValidationException extends IllegalArgumentException {
    private final String field;

    public ExpenseFormValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
