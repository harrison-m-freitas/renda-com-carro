package dev.harrison.rendacomcarro.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.harrison.rendacomcarro.expense.application.ExpenseAllocationPreview;
import dev.harrison.rendacomcarro.expense.application.ExpenseAllocationPreviewService;
import dev.harrison.rendacomcarro.expense.application.MileageAlert;
import dev.harrison.rendacomcarro.expense.application.MileageAlertSeverity;
import dev.harrison.rendacomcarro.expense.application.MonthlyMileageInferenceService;
import dev.harrison.rendacomcarro.expense.application.MonthlyMileagePreview;
import dev.harrison.rendacomcarro.expense.domain.MonthlyOdometerClosing;
import dev.harrison.rendacomcarro.expense.infrastructure.MonthlyOdometerClosingRepository;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpenseAllocationPreviewServiceTest {
    private MonthlyOdometerClosingRepository closings;
    private MonthlyMileageInferenceService inference;
    private VehicleService vehicles;
    private ExpenseAllocationPreviewService service;
    private UUID vehicleId;
    private YearMonth month;

    @BeforeEach
    void setUp() {
        closings = mock(MonthlyOdometerClosingRepository.class);
        inference = mock(MonthlyMileageInferenceService.class);
        vehicles = mock(VehicleService.class);
        service = new ExpenseAllocationPreviewService(closings, inference, vehicles);
        vehicleId = UUID.randomUUID();
        month = YearMonth.of(2026, 7);
        when(vehicles.getActive(vehicleId)).thenReturn(mock(Vehicle.class));
    }

    @Test
    void confirmedClosingWinsAndInferenceIsNotCalled() {
        MonthlyOdometerClosing closing = MonthlyOdometerClosing.create(
            mock(Vehicle.class), month,
            new BigDecimal("1000.0"), new BigDecimal("2000.0"),
            new BigDecimal("725.0"), null
        );
        when(closings.findByVehicleIdAndReferenceMonth(vehicleId, month.atDay(1)))
            .thenReturn(Optional.of(closing));

        ExpenseAllocationPreview result = service.preview(vehicleId, month);

        assertThat(result.status()).isEqualTo(ExpenseAllocationPreview.Status.CONFIRMED);
        assertThat(result.professionalPercentage()).isEqualByComparingTo("72.5000");
        assertThat(result.provisional()).isFalse();
        verifyNoInteractions(inference);
    }

    @Test
    void existingInferenceIsReturnedAsEstimateWhenUsable() {
        when(closings.findByVehicleIdAndReferenceMonth(vehicleId, month.atDay(1)))
            .thenReturn(Optional.empty());
        when(inference.infer(vehicleId, month)).thenReturn(preview(
            new BigDecimal("620.0"), new BigDecimal("430.0"),
            new BigDecimal("0.6935"), List.of()
        ));

        ExpenseAllocationPreview result = service.preview(vehicleId, month);

        assertThat(result.status()).isEqualTo(ExpenseAllocationPreview.Status.ESTIMATED);
        assertThat(result.professionalPercentage()).isEqualByComparingTo("69.3500");
        assertThat(result.provisional()).isTrue();
    }

    @Test
    void openShiftKeepsTheExistingEstimateAndExplainsThatItIsProvisional() {
        when(closings.findByVehicleIdAndReferenceMonth(vehicleId, month.atDay(1)))
            .thenReturn(Optional.empty());
        MileageAlert alert = new MileageAlert(
            "OPEN_SHIFT", MileageAlertSeverity.BLOCKING,
            "Existe turno em andamento no mês."
        );
        when(inference.infer(vehicleId, month)).thenReturn(preview(
            new BigDecimal("620.0"), new BigDecimal("430.0"),
            new BigDecimal("0.6935"), List.of(alert)
        ));

        ExpenseAllocationPreview result = service.preview(vehicleId, month);

        assertThat(result.status()).isEqualTo(ExpenseAllocationPreview.Status.ESTIMATED);
        assertThat(result.professionalPercentage()).isEqualByComparingTo("69.3500");
        assertThat(result.alerts()).containsExactly(alert);
    }

    @Test
    void inconsistentOrEmptyInferenceDoesNotInventPercentage() {
        when(closings.findByVehicleIdAndReferenceMonth(vehicleId, month.atDay(1)))
            .thenReturn(Optional.empty());
        MileageAlert alert = new MileageAlert(
            "PROFESSIONAL_EXCEEDS_TOTAL", MileageAlertSeverity.BLOCKING,
            "A soma dos quilômetros profissionais excede a distância total do mês."
        );
        when(inference.infer(vehicleId, month)).thenReturn(preview(
            new BigDecimal("620.0"), new BigDecimal("700.0"),
            BigDecimal.ZERO, List.of(alert)
        ));

        ExpenseAllocationPreview result = service.preview(vehicleId, month);

        assertThat(result.status()).isEqualTo(ExpenseAllocationPreview.Status.INSUFFICIENT_DATA);
        assertThat(result.professionalPercentage()).isNull();
        assertThat(result.alerts()).containsExactly(alert);
    }

    private MonthlyMileagePreview preview(
        BigDecimal total,
        BigDecimal professional,
        BigDecimal ratio,
        List<MileageAlert> alerts
    ) {
        return new MonthlyMileagePreview(
            vehicleId, month,
            new BigDecimal("1000.0"), new BigDecimal("1620.0"),
            total, professional, total.subtract(professional), ratio,
            null, null, 2, 2, 0, alerts, LocalDateTime.of(2026, 7, 14, 12, 0)
        );
    }
}
