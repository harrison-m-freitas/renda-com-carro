package dev.harrison.rendacomcarro.vehicle.web;

import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public class VehicleForm {
    @NotBlank
    private String name;

    @NotBlank
    private String make;

    @NotBlank
    private String model;

    @Min(1980)
    @Max(2100)
    private int year;

    @NotBlank
    @Pattern(regexp = "[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}", message = "Informe uma placa válida")
    private String plate;

    @NotNull
    private FuelType fuelType;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal initialOdometer = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }
    public BigDecimal getInitialOdometer() { return initialOdometer; }
    public void setInitialOdometer(BigDecimal initialOdometer) { this.initialOdometer = initialOdometer; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
}
