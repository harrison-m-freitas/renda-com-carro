package dev.harrison.rendacomcarro.fuel.web;

import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public class FuelingForm {
 @NotNull private UUID vehicleId;
 @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime fueledAt=LocalDateTime.now();
 @NotNull @DecimalMin("0.0") private BigDecimal odometer;
 private String station; @NotNull private FuelType fuelType=FuelType.FLEX;
 @NotNull @DecimalMin("0.001") private BigDecimal liters;
 @NotNull @DecimalMin("0.001") private BigDecimal pricePerLiter;
 @NotNull @DecimalMin("0.01") private BigDecimal totalAmount;
 private boolean fullTank; private String notes;
 public UUID getVehicleId(){return vehicleId;} public void setVehicleId(UUID v){vehicleId=v;} public LocalDateTime getFueledAt(){return fueledAt;} public void setFueledAt(LocalDateTime v){fueledAt=v;} public BigDecimal getOdometer(){return odometer;} public void setOdometer(BigDecimal v){odometer=v;} public String getStation(){return station;} public void setStation(String v){station=v;} public FuelType getFuelType(){return fuelType;} public void setFuelType(FuelType v){fuelType=v;} public BigDecimal getLiters(){return liters;} public void setLiters(BigDecimal v){liters=v;} public BigDecimal getPricePerLiter(){return pricePerLiter;} public void setPricePerLiter(BigDecimal v){pricePerLiter=v;} public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal v){totalAmount=v;} public boolean isFullTank(){return fullTank;} public void setFullTank(boolean v){fullTank=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
