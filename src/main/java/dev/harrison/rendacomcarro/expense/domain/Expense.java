package dev.harrison.rendacomcarro.expense.domain;

import dev.harrison.rendacomcarro.operation.domain.OperationalDay;
import dev.harrison.rendacomcarro.operation.domain.Shift;
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
@Table(name = "expense")
public class Expense {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operational_day_id")
    private OperationalDay operationalDay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private ExpenseCategory category;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "competence_date", nullable = false)
    private LocalDate competenceDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseClassification classification;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_method")
    private AllocationMethod allocationMethod;

    @Column(name = "professional_percentage", precision = 5, scale = 4)
    private BigDecimal professionalPercentage;

    @Column(name = "professional_fixed_amount", precision = 14, scale = 2)
    private BigDecimal professionalFixedAmount;

    @Column(name = "adjustment_reason")
    private String adjustmentReason;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Expense() {}

    public static Expense create(
        Vehicle vehicle,
        OperationalDay day,
        Shift shift,
        ExpenseCategory category,
        LocalDate expenseDate,
        LocalDate competenceDate,
        LocalDate paidDate,
        BigDecimal amount,
        ExpenseClassification classification,
        AllocationMethod method,
        BigDecimal professionalPercentage,
        BigDecimal professionalFixedAmount,
        String adjustmentReason,
        String notes
    ) {
        if (category == null || expenseDate == null || competenceDate == null) {
            throw new IllegalArgumentException("Categoria e datas são obrigatórias");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Valor deve ser maior que zero");
        }
        if (classification == null) {
            throw new IllegalArgumentException("Classificação é obrigatória");
        }

        AllocationMethod normalizedMethod = method;
        BigDecimal normalizedPercentage = professionalPercentage;
        BigDecimal normalizedFixedAmount = professionalFixedAmount;
        String normalizedReason = normalizeText(adjustmentReason);

        if (classification != ExpenseClassification.MIXED) {
            normalizedMethod = null;
            normalizedPercentage = null;
            normalizedFixedAmount = null;
            normalizedReason = null;
        } else {
            if (normalizedMethod == null) {
                throw new IllegalArgumentException("Método de rateio é obrigatório para gasto misto");
            }
            switch (normalizedMethod) {
                case MILEAGE_RATIO -> {
                    normalizedPercentage = null;
                    normalizedFixedAmount = null;
                    normalizedReason = null;
                }
                case MANUAL_PERCENTAGE -> {
                    if (normalizedPercentage == null || normalizedPercentage.signum() <= 0
                        || normalizedPercentage.compareTo(BigDecimal.ONE) >= 0) {
                        throw new IllegalArgumentException(
                            "Percentual profissional deve estar entre 0 e 1, sem os extremos"
                        );
                    }
                    if (normalizedReason == null) {
                        throw new IllegalArgumentException("Justificativa do ajuste é obrigatória");
                    }
                    normalizedFixedAmount = null;
                }
                case FIXED_AMOUNT -> {
                    if (normalizedFixedAmount == null || normalizedFixedAmount.signum() <= 0
                        || normalizedFixedAmount.compareTo(amount) >= 0) {
                        throw new IllegalArgumentException(
                            "Valor profissional fixo deve ser maior que zero e menor que o valor do gasto"
                        );
                    }
                    if (normalizedReason == null) {
                        throw new IllegalArgumentException("Justificativa do ajuste é obrigatória");
                    }
                    normalizedPercentage = null;
                }
            }
        }

        Expense expense = new Expense();
        expense.id = UUID.randomUUID();
        expense.vehicle = vehicle;
        expense.operationalDay = day;
        expense.shift = shift;
        expense.category = category;
        expense.expenseDate = expenseDate;
        expense.competenceDate = competenceDate;
        expense.paidDate = paidDate;
        expense.amount = DecimalPolicy.money(amount);
        expense.classification = classification;
        expense.allocationMethod = normalizedMethod;
        expense.professionalPercentage = normalizedPercentage;
        expense.professionalFixedAmount = normalizedFixedAmount == null
            ? null
            : DecimalPolicy.money(normalizedFixedAmount);
        expense.adjustmentReason = normalizedReason;
        expense.notes = notes;
        expense.status = "ACTIVE";
        expense.createdAt = LocalDateTime.now();
        return expense;
    }

    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static Expense mixed(BigDecimal amount, AllocationMethod method) {
        Expense expense = new Expense();
        expense.id = UUID.randomUUID();
        expense.amount = DecimalPolicy.money(amount);
        expense.classification = ExpenseClassification.MIXED;
        expense.allocationMethod = method;
        expense.status = "ACTIVE";
        expense.createdAt = LocalDateTime.now();
        return expense;
    }

    public void cancel() { status = "CANCELLED"; }
    public UUID getId() { return id; }
    public Vehicle getVehicle() { return vehicle; }
    public OperationalDay getOperationalDay() { return operationalDay; }
    public Shift getShift() { return shift; }
    public ExpenseCategory getCategory() { return category; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public LocalDate getCompetenceDate() { return competenceDate; }
    public LocalDate getPaidDate() { return paidDate; }
    public BigDecimal getAmount() { return amount; }
    public ExpenseClassification getClassification() { return classification; }
    public AllocationMethod getAllocationMethod() { return allocationMethod; }
    public BigDecimal getProfessionalPercentage() { return professionalPercentage; }
    public BigDecimal getProfessionalFixedAmount() { return professionalFixedAmount; }
    public String getAdjustmentReason() { return adjustmentReason; }
    public String getNotes() { return notes; }
    public String getStatus() { return status; }
}
