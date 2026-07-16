package dev.harrison.rendacomcarro.finance.application;

import dev.harrison.rendacomcarro.finance.domain.AcquisitionPlan;
import dev.harrison.rendacomcarro.finance.domain.FinancialObligation;
import dev.harrison.rendacomcarro.finance.domain.InstallmentStatus;
import dev.harrison.rendacomcarro.finance.domain.InterestRatePeriod;
import dev.harrison.rendacomcarro.finance.domain.ObligationCalculationMethod;
import dev.harrison.rendacomcarro.finance.domain.ObligationInstallment;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.domain.ObligationPayment;
import dev.harrison.rendacomcarro.finance.domain.ObligationType;
import dev.harrison.rendacomcarro.finance.infrastructure.FinancialObligationRepository;
import dev.harrison.rendacomcarro.finance.infrastructure.ObligationInstallmentRepository;
import dev.harrison.rendacomcarro.finance.infrastructure.ObligationPaymentRepository;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialObligationService {
    private final FinancialObligationRepository obligations;
    private final ObligationInstallmentRepository installments;
    private final ObligationPaymentRepository payments;
    private final VehicleService vehicles;
    private final AcquisitionPlanService acquisitionPlans;
    private final InstallmentScheduleCalculator calculator;

    public FinancialObligationService(
        FinancialObligationRepository obligations,
        ObligationInstallmentRepository installments,
        ObligationPaymentRepository payments,
        VehicleService vehicles,
        AcquisitionPlanService acquisitionPlans,
        InstallmentScheduleCalculator calculator
    ) {
        this.obligations = obligations;
        this.installments = installments;
        this.payments = payments;
        this.vehicles = vehicles;
        this.acquisitionPlans = acquisitionPlans;
        this.calculator = calculator;
    }

    public record CreateCommand(
        UUID vehicleId,
        UUID acquisitionPlanId,
        ObligationType type,
        ObligationMode mode,
        ObligationCalculationMethod calculationMethod,
        String creditor,
        BigDecimal principal,
        BigDecimal interestRateRatio,
        InterestRatePeriod interestRatePeriod,
        LocalDate startDate,
        LocalDate firstDueDate,
        Integer termMonths,
        BigDecimal installmentAmount,
        BigDecimal singlePaymentAmount,
        BigDecimal monthlyTarget,
        String notes
    ) {}

    @Transactional
    public FinancialObligation create(CreateCommand command) {
        AcquisitionPlan plan = command.acquisitionPlanId() == null
            ? null
            : acquisitionPlans.get(command.acquisitionPlanId());
        Vehicle vehicle = resolveVehicle(command.vehicleId(), plan);

        InstallmentScheduleCalculator.RepaymentSchedule schedule = buildSchedule(command);
        BigDecimal monthlyRate = schedule == null
            ? resolveFlexibleMonthlyRate(command)
            : schedule.monthlyRate();
        BigDecimal annualRate = monthlyRate == null
            ? null
            : calculator.annualEffectiveRate(monthlyRate);
        BigDecimal installment = schedule == null ? null : schedule.installmentAmount();
        BigDecimal plannedTotal = schedule == null ? null : schedule.totalAmount();
        BigDecimal plannedInterest = schedule == null ? null : schedule.totalInterest();

        FinancialObligation obligation = obligations.save(FinancialObligation.create(
            vehicle,
            plan,
            command.type(),
            command.mode(),
            command.calculationMethod(),
            command.creditor(),
            command.principal(),
            monthlyRate,
            annualRate,
            command.startDate(),
            command.mode() == ObligationMode.FLEXIBLE_PAYMENTS ? null : command.firstDueDate(),
            command.mode() == ObligationMode.FIXED_INSTALLMENTS ? command.termMonths() : null,
            installment,
            plannedTotal,
            plannedInterest,
            command.mode() == ObligationMode.FLEXIBLE_PAYMENTS ? command.monthlyTarget() : null,
            command.notes()
        ));

        if (schedule != null) {
            schedule.entries().forEach(entry -> installments.save(ObligationInstallment.create(
                obligation,
                entry.sequence(),
                entry.dueDate(),
                entry.principal(),
                entry.interest(),
                entry.total()
            )));
        }
        return obligation;
    }

    private InstallmentScheduleCalculator.RepaymentSchedule buildSchedule(CreateCommand command) {
        if (command.mode() == ObligationMode.FLEXIBLE_PAYMENTS) {
            return null;
        }
        if (command.mode() == ObligationMode.SINGLE_PAYMENT) {
            BigDecimal total = command.calculationMethod() == ObligationCalculationMethod.INTEREST_FREE
                ? command.principal()
                : command.singlePaymentAmount();
            return calculator.calculateSinglePayment(
                command.principal(), total, command.firstDueDate()
            );
        }
        if (command.termMonths() == null) {
            throw new ObligationCalculationException("termMonths", "Informe a quantidade de parcelas.");
        }
        return switch (command.calculationMethod()) {
            case INSTALLMENT_KNOWN -> calculator.calculateFromInstallment(
                command.principal(), command.installmentAmount(), command.termMonths(), command.firstDueDate()
            );
            case RATE_KNOWN -> calculator.calculateFromRate(
                command.principal(), command.interestRateRatio(), command.interestRatePeriod(),
                command.termMonths(), command.firstDueDate()
            );
            case INTEREST_FREE -> calculator.calculateInterestFree(
                command.principal(), command.termMonths(), command.firstDueDate()
            );
            default -> throw new ObligationCalculationException(
                "calculationMethod", "Escolha como os valores das parcelas serão calculados."
            );
        };
    }

    private BigDecimal resolveFlexibleMonthlyRate(CreateCommand command) {
        if (command.calculationMethod() == ObligationCalculationMethod.RATE_UNKNOWN) {
            return null;
        }
        if (command.calculationMethod() == ObligationCalculationMethod.INTEREST_FREE) {
            return BigDecimal.ZERO;
        }
        if (command.calculationMethod() == ObligationCalculationMethod.RATE_KNOWN) {
            return calculator.normalizeMonthlyRate(
                command.interestRateRatio(), command.interestRatePeriod()
            );
        }
        throw new ObligationCalculationException(
            "calculationMethod", "Escolha se há juros ou se a taxa ainda é desconhecida."
        );
    }

    private Vehicle resolveVehicle(UUID vehicleId, AcquisitionPlan plan) {
        Vehicle explicit = vehicleId == null ? null : vehicles.get(vehicleId);
        if (plan == null || plan.getVehicle() == null) {
            return explicit;
        }
        if (explicit != null && !explicit.getId().equals(plan.getVehicle().getId())) {
            throw new IllegalArgumentException("A obrigação deve usar o mesmo veículo do plano de compra");
        }
        return plan.getVehicle();
    }

    @Transactional
    public ObligationPayment pay(
        UUID id,
        LocalDate date,
        BigDecimal principal,
        BigDecimal interest,
        BigDecimal extra,
        String reference,
        String notes
    ) {
        FinancialObligation obligation = get(id);
        String normalizedReference = PaymentReferenceNormalizer.normalize(reference);
        if (normalizedReference != null
            && payments.existsByObligationIdAndExternalReference(id, normalizedReference)) {
            throw new ObligationPaymentValidationException(
                "externalReference",
                "Já existe um pagamento com essa referência"
            );
        }

        BigDecimal principalPaid = zeroIfNull(principal);
        BigDecimal interestPaid = zeroIfNull(interest);
        BigDecimal extraPaid = zeroIfNull(extra);
        BigDecimal scheduledPayment = principalPaid.add(interestPaid);
        if (scheduledPayment.add(extraPaid).signum() <= 0) {
            throw new IllegalArgumentException("Informe um valor de pagamento maior que zero");
        }

        if (obligation.getMode() != ObligationMode.FLEXIBLE_PAYMENTS
            && scheduledPayment.signum() > 0) {
            BigDecimal remainingPayment = scheduledPayment;
            for (ObligationInstallment installment : installments
                .findAllByObligationIdAndStatusNotOrderByDueDateAsc(id, InstallmentStatus.PAID)) {
                remainingPayment = installment.applyPayment(remainingPayment);
                installments.save(installment);
                if (remainingPayment.signum() == 0) {
                    break;
                }
            }
            if (remainingPayment.signum() > 0) {
                throw new IllegalArgumentException(
                    "Pagamento excede o saldo das parcelas programadas"
                );
            }
        }

        BigDecimal reduction = principalPaid.add(extraPaid);
        if (reduction.signum() > 0) {
            obligation.applyPrincipal(reduction);
            obligations.save(obligation);
        }
        return payments.save(ObligationPayment.create(
            obligation,
            date,
            principalPaid,
            interestPaid,
            extraPaid,
            normalizedReference,
            notes
        ));
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Transactional(readOnly = true)
    public FinancialObligation get(UUID id) {
        return obligations.findOneById(id)
            .orElseThrow(() -> new IllegalArgumentException("Obrigação não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<FinancialObligation> list() {
        return obligations.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<ObligationInstallment> schedule(UUID id) {
        return installments.findAllByObligationIdOrderByDueDateAsc(id);
    }

    @Transactional(readOnly = true)
    public List<ObligationPayment> paymentHistory(UUID id) {
        return payments.findAllByObligationIdOrderByPaymentDateDesc(id);
    }
}
