package dev.harrison.rendacomcarro.shared.domain;

import java.math.BigDecimal;

public final class FlexibleDecimalParser {
    private FlexibleDecimalParser() {
    }

    public static BigDecimal parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim()
            .replace("\u00A0", "")
            .replace(" ", "");
        if (normalized.contains(",") && normalized.contains(".")) {
            normalized = normalized.replace(".", "").replace(",", ".");
        } else if (normalized.contains(",")) {
            normalized = normalized.replace(",", ".");
        } else if (normalized.matches("-?\\d{1,3}(\\.\\d{3})+")) {
            normalized = normalized.replace(".", "");
        }
        return new BigDecimal(normalized);
    }
}
