package dev.harrison.rendacomcarro.vehicle.web;

import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.Locale;

public class VehicleForm {
    private String name;

    @NotBlank(message = "Informe a marca do veículo")
    private String make;

    @NotBlank(message = "Informe o modelo do veículo")
    private String model;

    @Min(value = 1980, message = "Informe um ano a partir de 1980")
    @Max(value = 2100, message = "Informe um ano válido")
    private int year;

    @NotBlank(message = "Informe a placa do veículo")
    @Pattern(
        regexp = "[A-Z]{3}(?:[0-9]{4}|[0-9][A-Z][0-9]{2})",
        message = "Informe uma placa no formato ABC-1234 ou ABC1D23"
    )
    private String plate;

    @NotNull(message = "Selecione o tipo de combustível")
    private FuelType fuelType;

    @NotNull(message = "Informe o odômetro")
    @DecimalMin(value = "0.0", message = "O odômetro não pode ser negativo")
    private BigDecimal initialOdometer = BigDecimal.ZERO;

    @NotNull(message = "Informe o preço de compra")
    @DecimalMin(value = "0.0", message = "O preço de compra não pode ser negativo")
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    public String getName() { return name; }
    public void setName(String name) { this.name = normalizeText(name); }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = normalizeText(make); }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = normalizeText(model); }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) {
        this.plate = plate == null
            ? null
            : plate.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }
    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }
    public BigDecimal getInitialOdometer() { return initialOdometer; }
    public void setInitialOdometer(BigDecimal initialOdometer) { this.initialOdometer = initialOdometer; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    private static String normalizeText(String value) {
        return value == null ? null : value.trim();
    }
}
