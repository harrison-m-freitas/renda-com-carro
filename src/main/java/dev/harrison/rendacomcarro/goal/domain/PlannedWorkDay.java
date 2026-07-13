package dev.harrison.rendacomcarro.goal.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name="planned_work_day")
public class PlannedWorkDay {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="monthly_goal_id") private MonthlyGoal monthlyGoal;
 @Column(name="work_date",nullable=false) private LocalDate workDate;
 @Column(name="planned_hours",nullable=false,precision=6,scale=2) private BigDecimal plannedHours;
 @Column(nullable=false) private boolean available;
 protected PlannedWorkDay(){}
 public static PlannedWorkDay create(MonthlyGoal goal,LocalDate date,BigDecimal hours){if(goal==null||date==null||hours==null||hours.signum()<0)throw new IllegalArgumentException("Dia planejado inválido");if(date.getDayOfWeek()==DayOfWeek.SUNDAY)throw new IllegalArgumentException("Domingo não pode ser planejado no MVP");PlannedWorkDay d=new PlannedWorkDay();d.id=UUID.randomUUID();d.monthlyGoal=goal;d.workDate=date;d.plannedHours=hours.setScale(2);d.available=true;return d;}
 public UUID getId(){return id;} public LocalDate getWorkDate(){return workDate;} public BigDecimal getPlannedHours(){return plannedHours;} public boolean isAvailable(){return available;}
}
