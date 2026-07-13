package dev.harrison.rendacomcarro.vehicle.web;

import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Locale;

public class VehicleForm {
    @Size(max = 120)
    private String name;

    @NotBlank(message = "Informe a marca do veículo")
    @Size(max = 80)
    private String make;

    @NotBlank(message = "Informe o modelo do veículo")
    @Size(max = 80)
    private String model;

    @NotNull(message = "Informe o ano do veículo")
    @Min(value = 1980, message = "O ano deve ser igual ou posterior a 1980")
    @Max(value = 2100, message = "Informe um ano válido")
    private Integer year;

    @NotBlank(message = "Informe a placa do veículo")
    @Pattern(
        regexp = "[A-Z]{3}[0-9][A-Z0-9][0-9]{2}",
        message = "Informe uma placa no formato ABC-1234 ou ABC1D23"
    )
    private String plate;

    @NotNull(message = "Selecione o tipo de combustível")
    private FuelType fuelType;

    @NotNull(message = "Informe o odômetro do veículo")
    @DecimalMin(value = "0.0", message = "O odômetro não pode ser negativo")
    private BigDecimal initialOdometer = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "O preço de compra não pode ser negativo")
    private BigDecimal purchasePrice;

    public String getName() { return name; }
    public void setName(String name) { this.name = trimToNull(name); }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = trimToNull(make); }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = trimToNull(model); }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = normalizePlate(plate); }
    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }
    public BigDecimal getInitialOdometer() { return initialOdometer; }
    public void setInitialOdometer(BigDecimal initialOdometer) { this.initialOdometer = initialOdometer; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    private static String normalizePlate(String value) {
        if (value == null) {
            return null;
        }
        return value.trim()
            .replaceAll("[\\s-]", "")
            .toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
