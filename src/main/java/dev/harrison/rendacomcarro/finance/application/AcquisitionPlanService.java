package dev.harrison.rendacomcarro.finance.application;

import dev.harrison.rendacomcarro.finance.domain.AcquisitionPlan;
import dev.harrison.rendacomcarro.finance.domain.FinancialObligation;
import dev.harrison.rendacomcarro.finance.infrastructure.AcquisitionPlanRepository;
import dev.harrison.rendacomcarro.finance.infrastructure.FinancialObligationRepository;
import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcquisitionPlanService {
    private final AcquisitionPlanRepository plans;
    private final FinancialObligationRepository obligations;
    private final VehicleService vehicles;

    public AcquisitionPlanService(
        AcquisitionPlanRepository plans,
        FinancialObligationRepository obligations,
        VehicleService vehicles
    ) {
        this.plans = plans;
        this.obligations = obligations;
        this.vehicles = vehicles;
    }

    public record CreateCommand(
        UUID vehicleId,
        String title,
        BigDecimal purchaseAmount,
        BigDecimal ownResourcesAmount,
        LocalDate purchaseDate,
        String notes
    ) {}

    public record Summary(
        AcquisitionPlan plan,
        List<FinancialObligation> obligations,
        BigDecimal obligationPrincipalAmount,
        BigDecimal totalFundingAmount,
        BigDecimal remainingAmount,
        BigDecimal plannedRepaymentAmount,
        BigDecimal financingCostAmount
    ) {}

    @Transactional
    public AcquisitionPlan create(CreateCommand command) {
        var vehicle = command.vehicleId() == null ? null : vehicles.get(command.vehicleId());
        return plans.save(AcquisitionPlan.create(
            vehicle,
            command.title(),
            command.purchaseAmount(),
            command.ownResourcesAmount(),
            command.purchaseDate(),
            command.notes()
        ));
    }

    @Transactional(readOnly = true)
    public AcquisitionPlan get(UUID id) {
        return plans.findOneById(id)
            .orElseThrow(() -> new IllegalArgumentException("Plano de compra não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<AcquisitionPlan> list() {
        return plans.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Summary summary(UUID id) {
        AcquisitionPlan plan = get(id);
        List<FinancialObligation> linked = obligations
            .findAllByAcquisitionPlanIdOrderByCreatedAtAsc(id);
        BigDecimal principal = linked.stream()
            .map(FinancialObligation::getPrincipalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal repayment = linked.stream()
            .map(obligation -> obligation.getPlannedTotalAmount() == null
                ? obligation.getPrincipalAmount()
                : obligation.getPlannedTotalAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFunding = plan.getOwnResourcesAmount().add(principal);
        BigDecimal remaining = plan.getPurchaseAmount().subtract(totalFunding);
        BigDecimal cost = repayment.subtract(principal);
        return new Summary(
            plan,
            List.copyOf(linked),
            DecimalPolicy.money(principal),
            DecimalPolicy.money(totalFunding),
            DecimalPolicy.money(remaining),
            DecimalPolicy.money(repayment),
            DecimalPolicy.money(cost)
        );
    }
}
