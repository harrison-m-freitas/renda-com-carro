package dev.harrison.rendacomcarro.goal.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;

public class GoalForm {
    @NotNull @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth month = YearMonth.now();
    @NotNull @DecimalMin("0.00") private BigDecimal personalNetGoal;
    @NotNull @DecimalMin("0.00") private BigDecimal operationalGoal;
    @NotNull @DecimalMin("0.00") private BigDecimal plannedHours;
    @NotBlank private String plannedDates;
    public YearMonth getMonth(){return month;} public void setMonth(YearMonth v){month=v;}
    public BigDecimal getPersonalNetGoal(){return personalNetGoal;} public void setPersonalNetGoal(BigDecimal v){personalNetGoal=v;}
    public BigDecimal getOperationalGoal(){return operationalGoal;} public void setOperationalGoal(BigDecimal v){operationalGoal=v;}
    public BigDecimal getPlannedHours(){return plannedHours;} public void setPlannedHours(BigDecimal v){plannedHours=v;}
    public String getPlannedDates(){return plannedDates;} public void setPlannedDates(String v){plannedDates=v;}
}
