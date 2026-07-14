package dev.harrison.rendacomcarro.finance.application;

import dev.harrison.rendacomcarro.finance.domain.FinancialObligation;
import dev.harrison.rendacomcarro.finance.domain.InstallmentStatus;
import dev.harrison.rendacomcarro.finance.domain.ObligationInstallment;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.domain.ObligationPayment;
import dev.harrison.rendacomcarro.finance.domain.ObligationType;
import dev.harrison.rendacomcarro.finance.infrastructure.FinancialObligationRepository;
import dev.harrison.rendacomcarro.finance.infrastructure.ObligationInstallmentRepository;
import dev.harrison.rendacomcarro.finance.infrastructure.ObligationPaymentRepository;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
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
    private final InstallmentScheduleCalculator calculator;

    public FinancialObligationService(
        FinancialObligationRepository obligations,
        ObligationInstallmentRepository installments,
        ObligationPaymentRepository payments,
        VehicleService vehicles,
        InstallmentScheduleCalculator calculator
    ) {
        this.obligations = obligations;
        this.installments = installments;
        this.payments = payments;
        this.vehicles = vehicles;
        this.calculator = calculator;
    }

    public record CreateCommand(
        UUID vehicleId,
        ObligationType type,
        ObligationMode mode,
        String creditor,
        BigDecimal principal,
        BigDecimal annualRate,
        LocalDate startDate,
        LocalDate firstDueDate,
        Integer termMonths,
        BigDecimal plannedInstallment,
        BigDecimal monthlyTarget,
        String notes
    ) {}

    @Transactional
    public FinancialObligation create(CreateCommand command) {
        var vehicle = command.vehicleId() == null
            ? null
            : vehicles.get(command.vehicleId());
        var obligation = obligations.save(FinancialObligation.create(
            vehicle,
            command.type(),
            command.mode(),
            command.creditor(),
            command.principal(),
            command.annualRate(),
            command.startDate(),
            command.firstDueDate(),
            command.termMonths(),
            command.plannedInstallment(),
            command.monthlyTarget(),
            command.notes()
        ));
        if (command.mode() == ObligationMode.STRUCTURED) {
            calculator.calculate(
                command.principal(),
                command.annualRate() == null ? BigDecimal.ZERO : command.annualRate(),
                command.termMonths(),
                command.firstDueDate()
            ).forEach(entry -> installments.save(ObligationInstallment.create(
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
        if (reference != null && !reference.isBlank()
            && payments.existsByObligationIdAndExternalReference(id, reference)) {
            throw new IllegalArgumentException("Pagamento duplicado");
        }

        if (obligation.getMode() == ObligationMode.STRUCTURED) {
            BigDecimal remainingPayment = principal.add(interest);
            for (ObligationInstallment installment : installments
                .findAllByObligationIdAndStatusNotOrderByDueDateAsc(
                    id,
                    InstallmentStatus.PAID
                )) {
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

        BigDecimal reduction = principal.add(extra);
        if (reduction.signum() > 0) {
            obligation.applyPrincipal(reduction);
            obligations.save(obligation);
        }
        return payments.save(ObligationPayment.create(
            obligation,
            date,
            principal,
            interest,
            extra,
            reference,
            notes
        ));
    }

    @Transactional(readOnly = true)
    public FinancialObligation get(UUID id) {
        return obligations.findById(id)
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
