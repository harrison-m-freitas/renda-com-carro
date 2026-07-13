package dev.harrison.rendacomcarro.operation.web;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID; import org.springframework.format.annotation.DateTimeFormat;
public class OperationalDayForm {
 @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate date;
 @NotNull private UUID vehicleId;
 @NotNull @DecimalMin("0.00") private BigDecimal plannedGoal;
 @NotNull @DecimalMin("0.0") private BigDecimal initialOdometer;
 public LocalDate getDate(){return date;} public void setDate(LocalDate date){this.date=date;} public UUID getVehicleId(){return vehicleId;} public void setVehicleId(UUID vehicleId){this.vehicleId=vehicleId;} public BigDecimal getPlannedGoal(){return plannedGoal;} public void setPlannedGoal(BigDecimal plannedGoal){this.plannedGoal=plannedGoal;} public BigDecimal getInitialOdometer(){return initialOdometer;} public void setInitialOdometer(BigDecimal initialOdometer){this.initialOdometer=initialOdometer;}
}
