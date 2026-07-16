package dev.harrison.rendacomcarro.finance.domain;

import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "financial_obligation")
public class FinancialObligation {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acquisition_plan_id")
    private AcquisitionPlan acquisitionPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "obligation_type", nullable = false, length = 40)
    private ObligationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ObligationMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", nullable = false, length = 40)
    private ObligationCalculationMethod calculationMethod;

    @Column(nullable = false, length = 160)
    private String creditor;

    @Column(name = "principal_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "monthly_interest_rate", precision = 18, scale = 12)
    private BigDecimal monthlyInterestRate;

    @Column(name = "annual_effective_interest_rate", precision = 18, scale = 12)
    private BigDecimal annualEffectiveInterestRate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "first_due_date")
    private LocalDate firstDueDate;

    @Column(name = "term_months")
    private Integer termMonths;

    @Column(name = "installment_amount", precision = 14, scale = 2)
    private BigDecimal installmentAmount;

    @Column(name = "planned_total_amount", precision = 14, scale = 2)
    private BigDecimal plannedTotalAmount;

    @Column(name = "planned_interest_amount", precision = 14, scale = 2)
    private BigDecimal plannedInterestAmount;

    @Column(name = "current_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal currentBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ObligationStatus status;

    @Column(name = "monthly_target", precision = 14, scale = 2)
    private BigDecimal monthlyTarget;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected FinancialObligation() {}

    public static FinancialObligation create(
        Vehicle vehicle,
        AcquisitionPlan acquisitionPlan,
        ObligationType type,
        ObligationMode mode,
        ObligationCalculationMethod calculationMethod,
        String creditor,
        BigDecimal principal,
        BigDecimal monthlyInterestRate,
        BigDecimal annualEffectiveInterestRate,
        LocalDate startDate,
        LocalDate firstDueDate,
        Integer termMonths,
        BigDecimal installmentAmount,
        BigDecimal plannedTotalAmount,
        BigDecimal plannedInterestAmount,
        BigDecimal monthlyTarget,
        String notes
    ) {
        if (type == null || mode == null || calculationMethod == null) {
            throw new IllegalArgumentException("Informe o tipo e a forma de pagamento");
        }
        if (creditor == null || creditor.isBlank()) {
            throw new IllegalArgumentException("Informe para quem o valor será pago");
        }
        if (principal == null || principal.signum() <= 0) {
            throw new IllegalArgumentException("O valor emprestado ou financiado deve ser maior que zero");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Informe a data do contrato ou empréstimo");
        }

        FinancialObligation obligation = new FinancialObligation();
        obligation.id = UUID.randomUUID();
        obligation.vehicle = vehicle;
        obligation.acquisitionPlan = acquisitionPlan;
        obligation.type = type;
        obligation.mode = mode;
        obligation.calculationMethod = calculationMethod;
        obligation.creditor = creditor.trim();
        obligation.principalAmount = DecimalPolicy.money(principal);
        obligation.monthlyInterestRate = normalizeRate(monthlyInterestRate);
        obligation.annualEffectiveInterestRate = normalizeRate(annualEffectiveInterestRate);
        obligation.startDate = startDate;
        obligation.firstDueDate = firstDueDate;
        obligation.termMonths = termMonths;
        obligation.installmentAmount = normalizeMoney(installmentAmount);
        obligation.plannedTotalAmount = normalizeMoney(plannedTotalAmount);
        obligation.plannedInterestAmount = normalizeMoney(plannedInterestAmount);
        obligation.currentBalance = obligation.principalAmount;
        obligation.status = ObligationStatus.ACTIVE;
        obligation.monthlyTarget = normalizeMoney(monthlyTarget);
        obligation.notes = notes == null || notes.isBlank() ? null : notes.trim();
        obligation.createdAt = LocalDateTime.now();
        return obligation;
    }

    public void applyPrincipal(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.compareTo(currentBalance) > 0) {
            throw new IllegalArgumentException(
                "A amortização deve ser maior que zero e não pode ultrapassar o saldo"
            );
        }
        currentBalance = DecimalPolicy.money(currentBalance.subtract(amount));
        if (currentBalance.signum() == 0) {
            status = ObligationStatus.PAID;
        }
    }

    private static BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? null : DecimalPolicy.money(value);
    }

    private static BigDecimal normalizeRate(BigDecimal value) {
        return value == null ? null : value.setScale(12, java.math.RoundingMode.HALF_UP);
    }

    public UUID getId() { return id; }
    public Vehicle getVehicle() { return vehicle; }
    public AcquisitionPlan getAcquisitionPlan() { return acquisitionPlan; }
    public ObligationType getType() { return type; }
    public ObligationMode getMode() { return mode; }
    public ObligationCalculationMethod getCalculationMethod() { return calculationMethod; }
    public String getCreditor() { return creditor; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getMonthlyInterestRate() { return monthlyInterestRate; }
    public BigDecimal getAnnualEffectiveInterestRate() { return annualEffectiveInterestRate; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getFirstDueDate() { return firstDueDate; }
    public Integer getTermMonths() { return termMonths; }
    public BigDecimal getInstallmentAmount() { return installmentAmount; }
    public BigDecimal getPlannedTotalAmount() { return plannedTotalAmount; }
    public BigDecimal getPlannedInterestAmount() { return plannedInterestAmount; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public ObligationStatus getStatus() { return status; }
    public BigDecimal getMonthlyTarget() { return monthlyTarget; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** Compatibilidade temporária para relatórios antigos. */
    public BigDecimal getAnnualInterestRate() { return annualEffectiveInterestRate; }
}
