package dev.harrison.rendacomcarro.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "obligation_payment")
public class ObligationPayment {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obligation_id")
    private FinancialObligation obligation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_id")
    private ObligationInstallment installment;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "principal_paid", nullable = false, precision = 14, scale = 2)
    private BigDecimal principalPaid;

    @Column(name = "interest_paid", nullable = false, precision = 14, scale = 2)
    private BigDecimal interestPaid;

    @Column(name = "extra_amortization", nullable = false, precision = 14, scale = 2)
    private BigDecimal extraAmortization;

    @Column(name = "external_reference", length = 120)
    private String externalReference;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ObligationPayment() {
    }

    public static ObligationPayment create(
        FinancialObligation obligation,
        LocalDate date,
        BigDecimal principal,
        BigDecimal interest,
        BigDecimal extra,
        String reference,
        String notes
    ) {
        ObligationPayment payment = new ObligationPayment();
        payment.id = UUID.randomUUID();
        payment.obligation = obligation;
        payment.paymentDate = date;
        payment.principalPaid = principal;
        payment.interestPaid = interest;
        payment.extraAmortization = extra;
        payment.amount = principal.add(interest).add(extra);
        payment.externalReference = reference;
        payment.notes = notes;
        payment.createdAt = LocalDateTime.now();
        return payment;
    }

    public UUID getId() { return id; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getPrincipalPaid() { return principalPaid; }
    public BigDecimal getInterestPaid() { return interestPaid; }
    public BigDecimal getExtraAmortization() { return extraAmortization; }
    public String getExternalReference() { return externalReference; }
    public String getNotes() { return notes; }
}
