package dev.harrison.rendacomcarro.expense;

import dev.harrison.rendacomcarro.expense.application.MonthlyOdometerClosingService;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=mileage-owner",
    "APP_ADMIN_PASSWORD=mileage-owner-credential"
})
class MonthlyOdometerClosingServiceTest extends PostgresIntegrationTest {
    @Autowired private VehicleService vehicles;
    @Autowired private MonthlyOdometerClosingService service;

    @Test
    void closingCalculatesPersonalDistanceAndProfessionalRatio() {
        var vehicle = vehicles.create(new VehicleService.CreateVehicleCommand(
            "Carro do fechamento",
            "Toyota",
            "Etios",
            2017,
            "KM01A23",
            FuelType.FLEX,
            new BigDecimal("1000.0"),
            new BigDecimal("40000.00")
        ));

        var closing = service.create(new MonthlyOdometerClosingService.CreateCommand(
            vehicle.getId(),
            YearMonth.of(2036, 1),
            new BigDecimal("1000.0"),
            new BigDecimal("3500.0"),
            new BigDecimal("2000.0"),
            null
        ));

        assertThat(closing.getTotalKilometers()).isEqualByComparingTo("2500.0");
        assertThat(closing.getPersonalKilometers()).isEqualByComparingTo("500.0");
        assertThat(closing.getProfessionalPercentage()).isEqualByComparingTo("0.8000");

        assertThatThrownBy(() -> service.create(
            new MonthlyOdometerClosingService.CreateCommand(
                vehicle.getId(),
                YearMonth.of(2036, 1),
                new BigDecimal("1000.0"),
                new BigDecimal("3500.0"),
                new BigDecimal("2000.0"),
                null
            )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Já existe fechamento");
    }
}
