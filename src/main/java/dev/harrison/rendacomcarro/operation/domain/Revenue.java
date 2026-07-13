package dev.harrison.rendacomcarro.operation.domain;

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
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "revenue")
public class Revenue {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "platform_id", nullable = false)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RevenueType type;

    @Column(name = "competence_date", nullable = false)
    private LocalDate competenceDate;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(name = "gross_amount", precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "platform_fee", precision = 14, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "net_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "tip_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal tipAmount;

    @Column(name = "bonus_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal bonusAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataSource source;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Revenue() {}

    private Revenue(Shift shift, Platform platform, RevenueType type, LocalDate competenceDate,
                    LocalDate receivedDate, BigDecimal grossAmount, BigDecimal platformFee,
                    BigDecimal netAmount, BigDecimal tipAmount, BigDecimal bonusAmount,
                    DataSource source, String externalReference) {
        if (shift == null || platform == null) throw new IllegalArgumentException("Turno e plataforma são obrigatórios");
        if (type == null) throw new IllegalArgumentException("Tipo de receita é obrigatório");
        if (competenceDate == null) throw new IllegalArgumentException("Data de competência é obrigatória");
        if (source == null) throw new IllegalArgumentException("Origem é obrigatória");
        if (netAmount == null || netAmount.signum() <= 0) throw new IllegalArgumentException("Valor líquido deve ser maior que zero");

        BigDecimal tip = tipAmount == null ? BigDecimal.ZERO : tipAmount;
        BigDecimal bonus = bonusAmount == null ? BigDecimal.ZERO : bonusAmount;
        BigDecimal fee = platformFee == null ? BigDecimal.ZERO : platformFee;
        if (tip.signum() < 0 || bonus.signum() < 0 || fee.signum() < 0) {
            throw new IllegalArgumentException("Valores adicionais não podem ser negativos");
        }
        if (grossAmount != null) {
            BigDecimal minimumGross = netAmount.subtract(tip).subtract(bonus);
            if (grossAmount.compareTo(minimumGross) < 0) {
                throw new IllegalArgumentException("Valor bruto incompatível com o valor líquido");
            }
        }

        this.id = UUID.randomUUID();
        this.shift = shift;
        this.platform = platform;
        this.type = type;
        this.competenceDate = competenceDate;
        this.receivedDate = receivedDate;
        this.grossAmount = grossAmount == null ? null : DecimalPolicy.money(grossAmount);
        this.platformFee = DecimalPolicy.money(fee);
        this.netAmount = DecimalPolicy.money(netAmount);
        this.tipAmount = DecimalPolicy.money(tip);
        this.bonusAmount = DecimalPolicy.money(bonus);
        this.source = source;
        this.externalReference = externalReference == null || externalReference.isBlank() ? null : externalReference.trim();
        this.createdAt = LocalDateTime.now();
    }

    public static Revenue create(Shift shift, Platform platform, RevenueType type, LocalDate competenceDate,
                                 LocalDate receivedDate, BigDecimal grossAmount, BigDecimal platformFee,
                                 BigDecimal netAmount, BigDecimal tipAmount, BigDecimal bonusAmount,
                                 DataSource source, String externalReference) {
        return new Revenue(shift, platform, type, competenceDate, receivedDate, grossAmount, platformFee,
            netAmount, tipAmount, bonusAmount, source, externalReference);
    }

    public UUID getId() { return id; }
    public Shift getShift() { return shift; }
    public Platform getPlatform() { return platform; }
    public RevenueType getType() { return type; }
    public LocalDate getCompetenceDate() { return competenceDate; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public BigDecimal getPlatformFee() { return platformFee; }
    public BigDecimal getNetAmount() { return netAmount; }
    public BigDecimal getTipAmount() { return tipAmount; }
    public BigDecimal getBonusAmount() { return bonusAmount; }
    public DataSource getSource() { return source; }
    public String getExternalReference() { return externalReference; }
}
