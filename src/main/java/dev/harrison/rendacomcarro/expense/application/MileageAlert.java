package dev.harrison.rendacomcarro.expense.application;

public record MileageAlert(
    String code,
    MileageAlertSeverity severity,
    String message
) {
    public boolean blocking() {
        return severity == MileageAlertSeverity.BLOCKING;
    }
}
