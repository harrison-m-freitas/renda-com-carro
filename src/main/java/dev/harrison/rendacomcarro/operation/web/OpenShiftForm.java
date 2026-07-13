package dev.harrison.rendacomcarro.operation.web;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.*; import org.springframework.format.annotation.DateTimeFormat;
public class OpenShiftForm {
 @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime startedAt;
 @NotNull @DecimalMin("0.0") private BigDecimal initialOdometer;
 @NotBlank private String startRegion;
 @NotEmpty private Set<UUID> platformIds=new LinkedHashSet<>();
 public LocalDateTime getStartedAt(){return startedAt;} public void setStartedAt(LocalDateTime startedAt){this.startedAt=startedAt;} public BigDecimal getInitialOdometer(){return initialOdometer;} public void setInitialOdometer(BigDecimal initialOdometer){this.initialOdometer=initialOdometer;} public String getStartRegion(){return startRegion;} public void setStartRegion(String startRegion){this.startRegion=startRegion;} public Set<UUID> getPlatformIds(){return platformIds;} public void setPlatformIds(Set<UUID> platformIds){this.platformIds=platformIds;}
}
