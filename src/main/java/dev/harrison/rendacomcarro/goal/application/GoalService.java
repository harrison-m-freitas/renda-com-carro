package dev.harrison.rendacomcarro.goal.application;

import dev.harrison.rendacomcarro.goal.domain.*;
import dev.harrison.rendacomcarro.goal.infrastructure.*;
import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {
 private final MonthlyGoalRepository goals; private final PlannedWorkDayRepository days;
 public GoalService(MonthlyGoalRepository goals,PlannedWorkDayRepository days){this.goals=goals;this.days=days;}
 public GoalProjection project(YearMonth month,BigDecimal target,BigDecimal realized,Set<LocalDate> plannedDates){
  if(month==null||target==null||realized==null)throw new IllegalArgumentException("Dados da projeção são obrigatórios");
  long eligible=plannedDates==null?0:plannedDates.stream().filter(d->d!=null&&YearMonth.from(d).equals(month)&&d.getDayOfWeek()!=DayOfWeek.SUNDAY).distinct().count();
  BigDecimal remaining=target.subtract(realized).max(BigDecimal.ZERO); BigDecimal required=eligible==0?BigDecimal.ZERO.setScale(2):DecimalPolicy.money(remaining.divide(BigDecimal.valueOf(eligible),8,RoundingMode.HALF_UP));
  BigDecimal progress=target.signum()==0?new BigDecimal("100.00"):realized.multiply(new BigDecimal("100")).divide(target,2,RoundingMode.HALF_UP);
  GoalStatus status=progress.compareTo(new BigDecimal("100"))>=0?GoalStatus.ABOVE:eligible==0?GoalStatus.BELOW:GoalStatus.ON_TRACK;
  return new GoalProjection(DecimalPolicy.money(target),DecimalPolicy.money(realized),DecimalPolicy.money(remaining),(int)eligible,required,progress,status);
 }
 @Transactional public MonthlyGoal create(YearMonth month,BigDecimal personal,BigDecimal operational,BigDecimal hours,Set<LocalDate> dates){
  if(goals.findByReferenceMonth(month.atDay(1)).isPresent())throw new IllegalArgumentException("Meta mensal já cadastrada");
  MonthlyGoal goal=goals.save(MonthlyGoal.create(month,personal,operational,hours));
  if(dates!=null)dates.stream().sorted().map(d->PlannedWorkDay.create(goal,d,BigDecimal.ZERO)).forEach(days::save);
  return goal;
 }
 @Transactional(readOnly=true) public Optional<MonthlyGoal> find(YearMonth month){return goals.findByReferenceMonth(month.atDay(1));}
 @Transactional(readOnly=true) public List<PlannedWorkDay> plannedDays(UUID goalId){return days.findAllByMonthlyGoalIdOrderByWorkDateAsc(goalId);}
}
