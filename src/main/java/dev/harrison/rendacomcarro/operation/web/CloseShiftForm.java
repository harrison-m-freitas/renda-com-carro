package dev.harrison.rendacomcarro.operation.web;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.*; import org.springframework.format.annotation.DateTimeFormat;
public class CloseShiftForm {
 @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime endedAt;
 @NotNull @DecimalMin("0.0") private BigDecimal finalOdometer;
 @NotBlank private String endRegion;
 private String neighborhoodsText;
 public Set<String> neighborhoods(){if(neighborhoodsText==null||neighborhoodsText.isBlank()) return Set.of(); Set<String> values=new LinkedHashSet<>(); Arrays.stream(neighborhoodsText.split("[,\n]")).map(String::trim).filter(s->!s.isBlank()).forEach(values::add); return values;}
 public LocalDateTime getEndedAt(){return endedAt;} public void setEndedAt(LocalDateTime endedAt){this.endedAt=endedAt;} public BigDecimal getFinalOdometer(){return finalOdometer;} public void setFinalOdometer(BigDecimal finalOdometer){this.finalOdometer=finalOdometer;} public String getEndRegion(){return endRegion;} public void setEndRegion(String endRegion){this.endRegion=endRegion;} public String getNeighborhoodsText(){return neighborhoodsText;} public void setNeighborhoodsText(String neighborhoodsText){this.neighborhoodsText=neighborhoodsText;}
}
