package dev.harrison.rendacomcarro.dashboard.application;

import dev.harrison.rendacomcarro.expense.application.ExpenseAllocationService;
import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.Expense;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import dev.harrison.rendacomcarro.expense.domain.MonthlyMileageSnapshot;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseRepository;
import dev.harrison.rendacomcarro.expense.infrastructure.MonthlyOdometerClosingRepository;
import dev.harrison.rendacomcarro.finance.domain.ObligationStatus;
import dev.harrison.rendacomcarro.finance.infrastructure.FinancialObligationRepository;
import dev.harrison.rendacomcarro.finance.infrastructure.ObligationPaymentRepository;
import dev.harrison.rendacomcarro.goal.application.GoalService;
import dev.harrison.rendacomcarro.operation.application.RevenueService;
import dev.harrison.rendacomcarro.operation.application.ShiftService;
import dev.harrison.rendacomcarro.operation.domain.OperationalDayStatus;
import dev.harrison.rendacomcarro.operation.domain.Revenue;
import dev.harrison.rendacomcarro.operation.domain.Shift;
import dev.harrison.rendacomcarro.operation.domain.ShiftStatus;
import dev.harrison.rendacomcarro.operation.infrastructure.OperationalDayRepository;
import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");
    private static final BigDecimal ZERO_DISTANCE = new BigDecimal("0.0");

    private final OperationalDayRepository days;
    private final ShiftService shifts;
    private final RevenueService revenues;
    private final ExpenseRepository expenses;
    private final ExpenseAllocationService expenseAllocation;
    private final MonthlyOdometerClosingRepository odometerClosings;
    private final FinancialObligationRepository obligations;
    private final ObligationPaymentRepository obligationPayments;
    private final GoalService goals;

    public DashboardService(
        OperationalDayRepository days,
        ShiftService shifts,
        RevenueService revenues,
        ExpenseRepository expenses,
        ExpenseAllocationService expenseAllocation,
        MonthlyOdometerClosingRepository odometerClosings,
        FinancialObligationRepository obligations,
        ObligationPaymentRepository obligationPayments,
        GoalService goals
    ) {
        this.days = days;
        this.shifts = shifts;
        this.revenues = revenues;
        this.expenses = expenses;
        this.expenseAllocation = expenseAllocation;
        this.odometerClosings = odometerClosings;
        this.obligations = obligations;
        this.obligationPayments = obligationPayments;
        this.goals = goals;
    }

    @Transactional(readOnly = true)
    public DashboardSnapshot snapshot(LocalDate date) {
        var day = days.findAllByOrderByDateDesc().stream()
            .filter(candidate -> candidate.getDate().equals(date))
            .filter(candidate -> candidate.getStatus() != OperationalDayStatus.CANCELLED)
            .findFirst();

        List<Shift> dayShifts = day
            .map(value -> shifts.listByDay(value.getId()))
            .orElseGet(List::of);

        BigDecimal dailyRevenue = revenues.sumByCompetence(date);
        BigDecimal dailyProfessionalExpense = sumProfessionalExpenses(
            expenses.findAllByCompetenceDate(date)
        );
        BigDecimal dailyOperatingMargin = money(dailyRevenue.subtract(dailyProfessionalExpense));

        YearMonth month = YearMonth.from(date);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        BigDecimal monthlyCashRevenue = revenues.sumByReceivedDate(monthStart, monthEnd);
        BigDecimal monthlyPaidProfessionalExpense = sumProfessionalExpenses(
            expenses.findAllByPaidDateBetween(monthStart, monthEnd)
        );
        BigDecimal monthlyDebtPayments = obligationPayments
            .findAllByPaymentDateBetween(monthStart, monthEnd)
            .stream()
            .map(payment -> payment.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthlyPersonalCash = money(
            monthlyCashRevenue
                .subtract(monthlyPaidProfessionalExpense)
                .subtract(monthlyDebtPayments)
        );

        Duration workedDuration = dayShifts.stream()
            .map(Shift::getDuration)
            .reduce(Duration.ZERO, Duration::plus);
        BigDecimal professionalDistance = dayShifts.stream()
            .map(Shift::getDistance)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(1, RoundingMode.HALF_UP);
        BigDecimal workedHours = BigDecimal.valueOf(workedDuration.toMinutes())
            .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal revenuePerHour = workedHours.signum() == 0
            ? ZERO_MONEY
            : dailyRevenue.divide(workedHours, 2, RoundingMode.HALF_UP);
        BigDecimal revenuePerKilometer = professionalDistance.signum() == 0
            ? ZERO_MONEY
            : dailyRevenue.divide(professionalDistance, 2, RoundingMode.HALF_UP);

        Optional<DashboardSnapshot.CurrentShiftView> currentShift = dayShifts.stream()
            .filter(shift -> shift.getStatus() == ShiftStatus.OPEN)
            .findFirst()
            .map(this::currentShiftView);

        BigDecimal outstandingDebt = obligations.findAll().stream()
            .filter(obligation -> obligation.getStatus() == ObligationStatus.ACTIVE)
            .map(obligation -> obligation.getCurrentBalance())
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

        var monthlyGoal = goals.find(month);
        BigDecimal personalGoal = monthlyGoal
            .map(goal -> goal.getPersonalNetGoal())
            .orElse(ZERO_MONEY);
        Set<LocalDate> remainingPlannedDays = monthlyGoal
            .map(goal -> goals.plannedDays(goal.getId()).stream()
                .map(planned -> planned.getWorkDate())
                .filter(plannedDate -> !plannedDate.isBefore(date))
                .collect(Collectors.toSet()))
            .orElseGet(Set::of);
        BigDecimal requiredPerRemainingDay = goals.project(
            month,
            personalGoal,
            monthlyPersonalCash,
            remainingPlannedDays
        ).requiredPerDay();

        List<DashboardSnapshot.RecentExpenseView> recentExpenses = expenses
            .findAllByOrderByExpenseDateDesc()
            .stream()
            .filter(expense -> "ACTIVE".equals(expense.getStatus()))
            .limit(5)
            .map(expense -> new DashboardSnapshot.RecentExpenseView(
                expense.getExpenseDate(),
                expense.getCategory().getName(),
                expense.getAmount()
            ))
            .toList();

        return new DashboardSnapshot(
            date,
            currentShift,
            day.isPresent() && day.get().getStatus() == OperationalDayStatus.IN_PROGRESS,
            dailyRevenue,
            dailyOperatingMargin,
            dailyOperatingMargin,
            monthlyPersonalCash,
            personalGoal,
            requiredPerRemainingDay,
            workedDuration,
            professionalDistance,
            revenuePerHour,
            revenuePerKilometer,
            outstandingDebt,
            recentExpenses
        );
    }

    private DashboardSnapshot.CurrentShiftView currentShiftView(Shift shift) {
        BigDecimal revenue = revenues.listByShift(shift.getId()).stream()
            .map(Revenue::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal professionalExpense = sumProfessionalExpenses(
            expenses.findAllByShiftId(shift.getId())
        );
        return new DashboardSnapshot.CurrentShiftView(
            shift.getId(),
            shift.getStartedAt(),
            shift.getDuration(),
            revenue,
            shift.getDistance(),
            money(revenue.subtract(professionalExpense))
        );
    }

    private BigDecimal sumProfessionalExpenses(List<Expense> values) {
        return values.stream()
            .filter(expense -> "ACTIVE".equals(expense.getStatus()))
            .map(this::professionalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal professionalAmount(Expense expense) {
        MonthlyMileageSnapshot mileage = mileageFor(expense);
        return expenseAllocation.allocate(expense, mileage).professionalAmount();
    }

    private MonthlyMileageSnapshot mileageFor(Expense expense) {
        if (expense.getClassification() != ExpenseClassification.MIXED
            || expense.getAllocationMethod() != AllocationMethod.MILEAGE_RATIO) {
            return new MonthlyMileageSnapshot(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        LocalDate referenceMonth = YearMonth.from(expense.getCompetenceDate()).atDay(1);
        return odometerClosings
            .findByVehicleIdAndReferenceMonth(expense.getVehicle().getId(), referenceMonth)
            .map(closing -> new MonthlyMileageSnapshot(
                closing.getTotalKilometers(),
                closing.getProfessionalKilometers()
            ))
            // Sem fechamento mensal, assumir 100% profissional evita superestimar o lucro.
            .orElseGet(() -> new MonthlyMileageSnapshot(BigDecimal.ONE, BigDecimal.ONE));
    }

    private static BigDecimal money(BigDecimal value) {
        return DecimalPolicy.money(value);
    }
}
