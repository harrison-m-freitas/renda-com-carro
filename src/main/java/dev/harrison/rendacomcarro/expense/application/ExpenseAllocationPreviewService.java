package dev.harrison.rendacomcarro.expense.application;

import dev.harrison.rendacomcarro.expense.domain.MonthlyOdometerClosing;
import dev.harrison.rendacomcarro.expense.infrastructure.MonthlyOdometerClosingRepository;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseAllocationPreviewService {
    private final MonthlyOdometerClosingRepository closings;
    private final MonthlyMileageInferenceService inference;
    private final VehicleService vehicles;

    public ExpenseAllocationPreviewService(
        MonthlyOdometerClosingRepository closings,
        MonthlyMileageInferenceService inference,
        VehicleService vehicles
    ) {
        this.closings = closings;
        this.inference = inference;
        this.vehicles = vehicles;
    }

    @Transactional(readOnly = true)
    public ExpenseAllocationPreview preview(UUID vehicleId, YearMonth month) {
        if (vehicleId == null || month == null) {
            throw new IllegalArgumentException("Veículo e mês de referência são obrigatórios.");
        }
        vehicles.getActive(vehicleId);

        return closings.findByVehicleIdAndReferenceMonth(vehicleId, month.atDay(1))
            .map(closing -> confirmed(vehicleId, month, closing))
            .orElseGet(() -> estimatedOrInsufficient(vehicleId, month));
    }

    private ExpenseAllocationPreview confirmed(
        UUID vehicleId,
        YearMonth month,
        MonthlyOdometerClosing closing
    ) {
        return new ExpenseAllocationPreview(
            ExpenseAllocationPreview.Status.CONFIRMED,
            vehicleId,
            month,
            closing.getTotalKilometers(),
            closing.getProfessionalKilometers(),
            asDisplayPercentage(closing.getProfessionalPercentage()),
            false,
            List.of()
        );
    }

    private ExpenseAllocationPreview estimatedOrInsufficient(UUID vehicleId, YearMonth month) {
        MonthlyMileagePreview preview = inference.infer(vehicleId, month);
        boolean usable = preview.totalKilometers() != null
            && preview.totalKilometers().signum() > 0
            && preview.professionalPercentage() != null
            && preview.alerts().stream().noneMatch(this::preventsEstimate);

        return new ExpenseAllocationPreview(
            usable
                ? ExpenseAllocationPreview.Status.ESTIMATED
                : ExpenseAllocationPreview.Status.INSUFFICIENT_DATA,
            vehicleId,
            month,
            preview.totalKilometers(),
            preview.professionalKilometers(),
            usable ? asDisplayPercentage(preview.professionalPercentage()) : null,
            true,
            preview.alerts()
        );
    }

    private boolean preventsEstimate(MileageAlert alert) {
        if (!alert.blocking()) {
            return false;
        }
        return !"OPEN_SHIFT".equals(alert.code())
            && !"OPEN_OPERATIONAL_DAY".equals(alert.code());
    }

    private BigDecimal asDisplayPercentage(BigDecimal ratio) {
        return ratio.movePointRight(2).setScale(4);
    }
}
