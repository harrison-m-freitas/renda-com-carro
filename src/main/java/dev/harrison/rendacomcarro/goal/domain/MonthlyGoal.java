package dev.harrison.rendacomcarro.goal.domain;

import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@Table(name="monthly_goal")
public class MonthlyGoal {
 @Id private UUID id;
 @Column(name="reference_month",nullable=false,unique=true) private LocalDate referenceMonth;
 @Column(name="personal_net_goal",nullable=false,precision=14,scale=2) private BigDecimal personalNetGoal;
 @Column(name="operational_goal",nullable=false,precision=14,scale=2) private BigDecimal operationalGoal;
 @Column(name="planned_hours",nullable=false,precision=8,scale=2) private BigDecimal plannedHours;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
 protected MonthlyGoal(){}
 public static MonthlyGoal create(YearMonth month,BigDecimal personal,BigDecimal operational,BigDecimal hours){if(month==null||personal==null||operational==null||hours==null||personal.signum()<0||operational.signum()<0||hours.signum()<0)throw new IllegalArgumentException("Meta mensal inválida");MonthlyGoal g=new MonthlyGoal();g.id=UUID.randomUUID();g.referenceMonth=month.atDay(1);g.personalNetGoal=DecimalPolicy.money(personal);g.operationalGoal=DecimalPolicy.money(operational);g.plannedHours=hours.setScale(2);g.createdAt=LocalDateTime.now();g.updatedAt=g.createdAt;return g;}
 public UUID getId(){return id;} public YearMonth getMonth(){return YearMonth.from(referenceMonth);} public LocalDate getReferenceMonth(){return referenceMonth;} public BigDecimal getPersonalNetGoal(){return personalNetGoal;} public BigDecimal getOperationalGoal(){return operationalGoal;} public BigDecimal getPlannedHours(){return plannedHours;}
}
