package dev.harrison.rendacomcarro.expense.application;

import dev.harrison.rendacomcarro.expense.domain.*;
import dev.harrison.rendacomcarro.expense.infrastructure.*;
import dev.harrison.rendacomcarro.operation.application.OperationalDayService;
import dev.harrison.rendacomcarro.operation.application.ShiftService;
import dev.harrison.rendacomcarro.operation.domain.OperationalDay;
import dev.harrison.rendacomcarro.operation.domain.Shift;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {
 private final ExpenseRepository expenses; private final ExpenseCategoryRepository categories; private final VehicleService vehicles; private final OperationalDayService days; private final ShiftService shifts;
 public ExpenseService(ExpenseRepository expenses,ExpenseCategoryRepository categories,VehicleService vehicles,OperationalDayService days,ShiftService shifts){this.expenses=expenses;this.categories=categories;this.vehicles=vehicles;this.days=days;this.shifts=shifts;}
 public record CreateExpenseCommand(UUID vehicleId,UUID operationalDayId,UUID shiftId,UUID categoryId,LocalDate expenseDate,LocalDate competenceDate,LocalDate paidDate,BigDecimal amount,ExpenseClassification classification,AllocationMethod allocationMethod,BigDecimal professionalPercentage,BigDecimal professionalFixedAmount,String adjustmentReason,String notes){}
 @Transactional public Expense create(CreateExpenseCommand c){
  var vehicle=vehicles.getActive(c.vehicleId()); ExpenseCategory category=categories.findById(c.categoryId())
    .filter(ExpenseCategory::isActive)
    .orElseThrow(()->new IllegalArgumentException("Categoria ativa não encontrada"));
  OperationalDay day=c.operationalDayId()==null?null:days.get(c.operationalDayId()); Shift shift=c.shiftId()==null?null:shifts.get(c.shiftId());
  if(day!=null&&!day.getVehicle().getId().equals(vehicle.getId())) throw new IllegalArgumentException("Dia não pertence ao veículo");
  if(shift!=null&&(day==null||!shift.getOperationalDay().getId().equals(day.getId()))) throw new IllegalArgumentException("Turno não pertence ao dia informado");
  return expenses.save(Expense.create(vehicle,day,shift,category,c.expenseDate(),c.competenceDate(),c.paidDate(),c.amount(),c.classification(),c.allocationMethod(),c.professionalPercentage(),c.professionalFixedAmount(),c.adjustmentReason(),c.notes()));
 }
 @Transactional(readOnly=true) public List<Expense> listAll(){return expenses.findAllByOrderByExpenseDateDesc();}
 @Transactional public void cancel(UUID id){Expense e=expenses.findById(id).orElseThrow(()->new IllegalArgumentException("Gasto não encontrado"));e.cancel();expenses.save(e);}
}
