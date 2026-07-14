package dev.harrison.rendacomcarro.finance.domain;

import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
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
import java.util.UUID;

@Entity
@Table(name = "obligation_installment")
public class ObligationInstallment {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obligation_id")
    private FinancialObligation obligation;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "expected_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstallmentStatus status;

    protected ObligationInstallment() {}

    public static ObligationInstallment create(
        FinancialObligation obligation,
        int sequence,
        LocalDate dueDate,
        BigDecimal principal,
        BigDecimal interest,
        BigDecimal total
    ) {
        ObligationInstallment installment = new ObligationInstallment();
        installment.id = UUID.randomUUID();
        installment.obligation = obligation;
        installment.sequenceNumber = sequence;
        installment.dueDate = dueDate;
        installment.principalAmount = DecimalPolicy.money(principal);
        installment.interestAmount = DecimalPolicy.money(interest);
        installment.expectedAmount = DecimalPolicy.money(total);
        installment.paidAmount = BigDecimal.ZERO.setScale(2);
        installment.status = InstallmentStatus.PENDING;
        return installment;
    }

    public BigDecimal remainingAmount() {
        return DecimalPolicy.money(expectedAmount.subtract(paidAmount).max(BigDecimal.ZERO));
    }

    public BigDecimal applyPayment(BigDecimal requestedAmount) {
        if (requestedAmount == null || requestedAmount.signum() < 0) {
            throw new IllegalArgumentException("Pagamento da parcela inválido");
        }
        if (requestedAmount.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal applied = requestedAmount.min(remainingAmount());
        paidAmount = DecimalPolicy.money(paidAmount.add(applied));
        status = remainingAmount().signum() == 0
            ? InstallmentStatus.PAID
            : InstallmentStatus.PARTIALLY_PAID;
        return DecimalPolicy.money(requestedAmount.subtract(applied));
    }

    public UUID getId() { return id; }
    public FinancialObligation getObligation() { return obligation; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public InstallmentStatus getStatus() { return status; }
}
