package dev.harrison.rendacomcarro.finance.web;

import dev.harrison.rendacomcarro.finance.domain.InterestRatePeriod;
import dev.harrison.rendacomcarro.finance.domain.ObligationCalculationMethod;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.domain.ObligationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public class ObligationForm {
    @NotBlank(message = "O identificador do rascunho é obrigatório")
    private String draftKey;
    private UUID acquisitionPlanId;
    private UUID vehicleId;

    @NotNull(message = "Escolha o tipo de compromisso")
    private ObligationType type = ObligationType.FAMILY_LOAN;

    @NotNull(message = "Escolha como o pagamento será feito")
    private ObligationMode mode = ObligationMode.FLEXIBLE_PAYMENTS;

    @NotNull(message = "Escolha quais informações você possui")
    private ObligationCalculationMethod calculationMethod = ObligationCalculationMethod.INTEREST_FREE;

    @NotBlank(message = "Informe para quem o valor será pago")
    @Size(max = 160, message = "Use no máximo 160 caracteres")
    private String creditor;

    @NotNull(message = "Informe o valor emprestado ou financiado")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    private BigDecimal principalAmount;

    private BigDecimal interestRatePercent;
    private InterestRatePeriod interestRatePeriod = InterestRatePeriod.MONTHLY;

    @NotNull(message = "Informe a data do contrato ou empréstimo")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate = LocalDate.now();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate firstDueDate;
    private Integer termMonths;
    private BigDecimal installmentAmount;
    private BigDecimal singlePaymentAmount;
    private BigDecimal monthlyTarget;

    @Size(max = 4000, message = "Use no máximo 4.000 caracteres")
    private String notes;

    public String draftContextKey() {
        return draftKey == null ? "" : draftKey.trim();
    }

    public BigDecimal interestRateRatio() {
        return interestRatePercent == null ? null : interestRatePercent.movePointLeft(2);
    }

    public String getDraftKey() { return draftKey; }
    public void setDraftKey(String draftKey) { this.draftKey = draftKey; }
    public UUID getAcquisitionPlanId() { return acquisitionPlanId; }
    public void setAcquisitionPlanId(UUID acquisitionPlanId) { this.acquisitionPlanId = acquisitionPlanId; }
    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }
    public ObligationType getType() { return type; }
    public void setType(ObligationType type) { this.type = type; }
    public ObligationMode getMode() { return mode; }
    public void setMode(ObligationMode mode) { this.mode = mode; }
    public ObligationCalculationMethod getCalculationMethod() { return calculationMethod; }
    public void setCalculationMethod(ObligationCalculationMethod calculationMethod) { this.calculationMethod = calculationMethod; }
    public String getCreditor() { return creditor; }
    public void setCreditor(String creditor) { this.creditor = creditor; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestRatePercent() { return interestRatePercent; }
    public void setInterestRatePercent(BigDecimal interestRatePercent) { this.interestRatePercent = interestRatePercent; }
    public InterestRatePeriod getInterestRatePeriod() { return interestRatePeriod; }
    public void setInterestRatePeriod(InterestRatePeriod interestRatePeriod) { this.interestRatePeriod = interestRatePeriod; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getFirstDueDate() { return firstDueDate; }
    public void setFirstDueDate(LocalDate firstDueDate) { this.firstDueDate = firstDueDate; }
    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }
    public BigDecimal getInstallmentAmount() { return installmentAmount; }
    public void setInstallmentAmount(BigDecimal installmentAmount) { this.installmentAmount = installmentAmount; }
    public BigDecimal getSinglePaymentAmount() { return singlePaymentAmount; }
    public void setSinglePaymentAmount(BigDecimal singlePaymentAmount) { this.singlePaymentAmount = singlePaymentAmount; }
    public BigDecimal getMonthlyTarget() { return monthlyTarget; }
    public void setMonthlyTarget(BigDecimal monthlyTarget) { this.monthlyTarget = monthlyTarget; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
