package dev.harrison.rendacomcarro.operation;

import dev.harrison.rendacomcarro.operation.application.OperationalDayService;
import dev.harrison.rendacomcarro.operation.application.RevenueService;
import dev.harrison.rendacomcarro.operation.application.ShiftService;
import dev.harrison.rendacomcarro.operation.domain.DataSource;
import dev.harrison.rendacomcarro.operation.domain.Platform;
import dev.harrison.rendacomcarro.operation.domain.RevenueType;
import dev.harrison.rendacomcarro.operation.infrastructure.PlatformRepository;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=revenue-owner",
    "APP_ADMIN_PASSWORD=revenue-owner-credential"
})
class RevenueServiceTest extends PostgresIntegrationTest {
    @Autowired VehicleService vehicleService;
    @Autowired OperationalDayService dayService;
    @Autowired ShiftService shiftService;
    @Autowired RevenueService service;
    @Autowired PlatformRepository platformRepository;

    private java.util.UUID shiftId;
    private java.util.UUID uberId;

    @BeforeEach
    void setUp() {
        var vehicle = vehicleService.create(new VehicleService.CreateVehicleCommand(
            "Carro receitas " + java.util.UUID.randomUUID(), "Fiat", "Argo", 2020,
            "R" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase(), FuelType.FLEX,
            new BigDecimal("1000.0"), new BigDecimal("50000.00")));
        vehicleService.activate(vehicle.getId());
        var day = dayService.openDay(LocalDate.now(), vehicle.getId(), new BigDecimal("200.00"), new BigDecimal("1000.0"));
        uberId = platformRepository.findByCode("UBER").map(Platform::getId).orElseThrow();
        shiftId = shiftService.openShift(day.getId(), LocalDateTime.now().minusHours(1),
            new BigDecimal("1000.0"), "Centro", Set.of(uberId)).getId();
    }

    @Test
    void revenueRequiresNetAmountButGrossIsOptional() {
        var revenue = service.create(new RevenueService.CreateRevenueCommand(
            shiftId, null, uberId, RevenueType.CONSOLIDATED,
            LocalDate.of(2026, 7, 12), null, null, null,
            new BigDecimal("168.50"), BigDecimal.ZERO, BigDecimal.ZERO,
            DataSource.MANUAL, null));
        assertThat(revenue.getNetAmount()).isEqualByComparingTo("168.50");
    }

    @Test
    void receivedRevenueAppearsInCashOnlyOnReceivedDate() {
        service.create(new RevenueService.CreateRevenueCommand(
            shiftId, null, uberId, RevenueType.CONSOLIDATED,
            LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 15),
            null, null, new BigDecimal("168.50"),
            BigDecimal.ZERO, BigDecimal.ZERO, DataSource.MANUAL, null));

        assertThat(service.sumByCompetence(LocalDate.of(2026, 7, 12))).isEqualByComparingTo("168.50");
        assertThat(service.sumByReceivedDate(LocalDate.of(2026, 7, 12))).isEqualByComparingTo("0.00");
        assertThat(service.sumByReceivedDate(LocalDate.of(2026, 7, 15))).isEqualByComparingTo("168.50");
    }
}
