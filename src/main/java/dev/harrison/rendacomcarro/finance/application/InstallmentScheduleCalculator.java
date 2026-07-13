package dev.harrison.rendacomcarro.finance.application;
import java.math.*;import java.time.LocalDate;import java.util.*;import org.springframework.stereotype.Service;
@Service public class InstallmentScheduleCalculator {
 public record ScheduleEntry(int sequence,LocalDate dueDate,BigDecimal principal,BigDecimal interest,BigDecimal total){}
 public List<ScheduleEntry> calculate(BigDecimal principal,BigDecimal annualRate,int months,LocalDate firstDueDate){
  if(principal==null||principal.signum()<=0||annualRate==null||annualRate.signum()<0||months<=0||firstDueDate==null)throw new IllegalArgumentException("Dados do cronograma inválidos");
  BigDecimal monthly=annualRate.divide(new BigDecimal("12"),12,RoundingMode.HALF_UP); BigDecimal payment;
  if(monthly.signum()==0)payment=principal.divide(BigDecimal.valueOf(months),2,RoundingMode.HALF_UP); else {BigDecimal factor=BigDecimal.ONE.add(monthly).pow(months);payment=principal.multiply(monthly).multiply(factor).divide(factor.subtract(BigDecimal.ONE),2,RoundingMode.HALF_UP);}
  List<ScheduleEntry> result=new ArrayList<>();BigDecimal balance=principal;
  for(int i=1;i<=months;i++){BigDecimal interest=balance.multiply(monthly).setScale(2,RoundingMode.HALF_UP);BigDecimal principalPart=(i==months?balance:payment.subtract(interest)).setScale(2,RoundingMode.HALF_UP);BigDecimal total=principalPart.add(interest).setScale(2,RoundingMode.HALF_UP);result.add(new ScheduleEntry(i,firstDueDate.plusMonths(i-1),principalPart,interest,total));balance=balance.subtract(principalPart);}
  return result;
 }
}
