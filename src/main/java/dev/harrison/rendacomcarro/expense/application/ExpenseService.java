package dev.harrison.rendacomcarro.expense.application;

import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.Expense;
import dev.harrison.rendacomcarro.expense.domain.ExpenseCategory;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseCategoryRepository;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseRepository;
import dev.harrison.rendacomcarro.operation.application.OperationalDayService;
import dev.harrison.rendacomcarro.operation.application.ShiftService;
import dev.harrison.rendacomcarro.operation.domain.OperationalDay;
import dev.harrison.rendacomcarro.operation.domain.Shift;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {
    private final ExpenseRepository expenses;
    private final ExpenseCategoryRepository categories;
    private final VehicleService vehicles;
    private final OperationalDayService days;
    private final ShiftService shifts;

    public ExpenseService(
        ExpenseRepository expenses,
        ExpenseCategoryRepository categories,
        VehicleService vehicles,
        OperationalDayService days,
        ShiftService shifts
    ) {
        this.expenses = expenses;
        this.categories = categories;
        this.vehicles = vehicles;
        this.days = days;
        this.shifts = shifts;
    }

    public record CreateExpenseCommand(
        UUID vehicleId,
        UUID operationalDayId,
        UUID shiftId,
        UUID categoryId,
        LocalDate expenseDate,
        LocalDate competenceDate,
        LocalDate paidDate,
        BigDecimal amount,
        ExpenseClassification classification,
        AllocationMethod allocationMethod,
        BigDecimal professionalPercentage,
        BigDecimal professionalFixedAmount,
        String adjustmentReason,
        String notes
    ) {}

    @Transactional
    public Expense create(CreateExpenseCommand command) {
        Vehicle vehicle = command.vehicleId() == null
            ? null
            : vehicles.get(command.vehicleId());
        ExpenseCategory category = categories.findById(command.categoryId())
            .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));
        OperationalDay day = command.operationalDayId() == null
            ? null
            : days.get(command.operationalDayId());
        Shift shift = command.shiftId() == null
            ? null
            : shifts.get(command.shiftId());

        if (day != null && (vehicle == null
            || !day.getVehicle().getId().equals(vehicle.getId()))) {
            throw new IllegalArgumentException("Dia não pertence ao veículo");
        }
        if (shift != null && (day == null
            || !shift.getOperationalDay().getId().equals(day.getId()))) {
            throw new IllegalArgumentException("Turno não pertence ao dia informado");
        }

        return expenses.save(Expense.create(
            vehicle,
            day,
            shift,
            category,
            command.expenseDate(),
            command.competenceDate(),
            command.paidDate(),
            command.amount(),
            command.classification(),
            command.allocationMethod(),
            command.professionalPercentage(),
            command.professionalFixedAmount(),
            command.adjustmentReason(),
            command.notes()
        ));
    }

    @Transactional(readOnly = true)
    public List<Expense> listAll() {
        return expenses.findAllByOrderByExpenseDateDesc();
    }

    @Transactional
    public void cancel(UUID id) {
        Expense expense = expenses.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Gasto não encontrado"));
        expense.cancel();
        expenses.save(expense);
    }
}
