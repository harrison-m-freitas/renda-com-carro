package dev.harrison.rendacomcarro.finance.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface InstallmentSuggestionProjection {
    UUID getInstallmentId();
    UUID getObligationId();
    UUID getVehicleId();
    String getVehicleName();
    String getCreditor();
    LocalDate getDueDate();
    BigDecimal getExpectedAmount();
    BigDecimal getPaidAmount();
}
