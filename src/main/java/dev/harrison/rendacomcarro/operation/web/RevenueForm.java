package dev.harrison.rendacomcarro.operation.web;

import dev.harrison.rendacomcarro.operation.domain.DataSource;
import dev.harrison.rendacomcarro.operation.domain.RevenueType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public class RevenueForm {
    @NotNull
    private UUID platformId;
    @NotNull
    private RevenueType type = RevenueType.CONSOLIDATED;
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate competenceDate = LocalDate.now();
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate receivedDate;
    @DecimalMin("0.00")
    private BigDecimal grossAmount;
    @DecimalMin("0.00")
    private BigDecimal platformFee;
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal netAmount;
    @DecimalMin("0.00")
    private BigDecimal tipAmount = BigDecimal.ZERO;
    @DecimalMin("0.00")
    private BigDecimal bonusAmount = BigDecimal.ZERO;
    @NotNull
    private DataSource source = DataSource.MANUAL;
    private String externalReference;

    public UUID getPlatformId() { return platformId; }
    public void setPlatformId(UUID platformId) { this.platformId = platformId; }
    public RevenueType getType() { return type; }
    public void setType(RevenueType type) { this.type = type; }
    public LocalDate getCompetenceDate() { return competenceDate; }
    public void setCompetenceDate(LocalDate competenceDate) { this.competenceDate = competenceDate; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public BigDecimal getTipAmount() { return tipAmount; }
    public void setTipAmount(BigDecimal tipAmount) { this.tipAmount = tipAmount; }
    public BigDecimal getBonusAmount() { return bonusAmount; }
    public void setBonusAmount(BigDecimal bonusAmount) { this.bonusAmount = bonusAmount; }
    public DataSource getSource() { return source; }
    public void setSource(DataSource source) { this.source = source; }
    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }
}
