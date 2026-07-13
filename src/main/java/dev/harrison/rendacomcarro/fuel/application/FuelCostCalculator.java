package dev.harrison.rendacomcarro.fuel.application;

import dev.harrison.rendacomcarro.fuel.domain.Fueling;
import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FuelCostCalculator {
    public BigDecimal estimateCost(BigDecimal distanceKm, BigDecimal kilometersPerLiter, BigDecimal pricePerLiter) {
        if (distanceKm == null || kilometersPerLiter == null || pricePerLiter == null
            || distanceKm.signum() < 0 || kilometersPerLiter.signum() <= 0 || pricePerLiter.signum() <= 0) {
            throw new IllegalArgumentException("Dados de consumo inválidos");
        }
        return DecimalPolicy.money(distanceKm.divide(kilometersPerLiter, 8, RoundingMode.HALF_UP).multiply(pricePerLiter));
    }

    public Optional<BigDecimal> consumptionBetween(Fueling previous, Fueling current) {
        if (previous == null || current == null || !previous.isFullTank() || !current.isFullTank()) return Optional.empty();
        BigDecimal distance = current.getOdometer().subtract(previous.getOdometer());
        if (distance.signum() <= 0 || current.getLiters().signum() <= 0) return Optional.empty();
        return Optional.of(distance.divide(current.getLiters(), 2, RoundingMode.HALF_UP));
    }
}
