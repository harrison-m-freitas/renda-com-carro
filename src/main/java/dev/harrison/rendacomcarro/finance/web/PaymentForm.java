package dev.harrison.rendacomcarro.finance.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class PaymentForm {
    @NotNull(message = "Informe a data do pagamento")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date = LocalDate.now();

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal principal = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal interest = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal extra = BigDecimal.ZERO;

    @Size(max = 120, message = "Use no máximo 120 caracteres na referência")
    private String externalReference;

    private String notes;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getPrincipal() { return principal; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }
    public BigDecimal getInterest() { return interest; }
    public void setInterest(BigDecimal interest) { this.interest = interest; }
    public BigDecimal getExtra() { return extra; }
    public void setExtra(BigDecimal extra) { this.extra = extra; }
    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
