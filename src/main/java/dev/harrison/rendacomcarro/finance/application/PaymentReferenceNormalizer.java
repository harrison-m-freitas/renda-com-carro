package dev.harrison.rendacomcarro.finance.application;

public final class PaymentReferenceNormalizer {
    public static final int MAX_LENGTH = 120;

    private PaymentReferenceNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_LENGTH) {
            throw new ObligationPaymentValidationException(
                "externalReference",
                "Use no máximo 120 caracteres na referência"
            );
        }
        return normalized;
    }
}
