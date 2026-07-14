package dev.harrison.rendacomcarro.goal.application;

import dev.harrison.rendacomcarro.goal.domain.GoalProjection;
import dev.harrison.rendacomcarro.goal.domain.GoalStatus;
import dev.harrison.rendacomcarro.goal.domain.MonthlyGoal;
import dev.harrison.rendacomcarro.goal.domain.PlannedWorkDay;
import dev.harrison.rendacomcarro.goal.domain.WorkloadPeriodicity;
import dev.harrison.rendacomcarro.goal.infrastructure.MonthlyGoalRepository;
import dev.harrison.rendacomcarro.goal.infrastructure.PlannedWorkDayRepository;
import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import dev.harrison.rendacomcarro.vehicle.domain.VehicleStatus;
import dev.harrison.rendacomcarro.vehicle.infrastructure.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {
    private final MonthlyGoalRepository goals;
    private final PlannedWorkDayRepository days;
    private final WorkloadPlannerService workloadPlanner;
    private final VehicleRepository vehicles;

    public GoalService(MonthlyGoalRepository goals, PlannedWorkDayRepository days) {
        this(goals, days, new WorkloadPlannerService(), null);
    }

    @Autowired
    public GoalService(
        MonthlyGoalRepository goals,
        PlannedWorkDayRepository days,
        WorkloadPlannerService workloadPlanner,
        VehicleRepository vehicles
    ) {
        this.goals = goals;
        this.days = days;
        this.workloadPlanner = workloadPlanner;
        this.vehicles = vehicles;
    }

    public GoalProjection project(
        YearMonth month,
        BigDecimal target,
        BigDecimal realized,
        Set<LocalDate> plannedDates
    ) {
        if (month == null || target == null || realized == null) {
            throw new IllegalArgumentException("Dados da projeção são obrigatórios");
        }
        long eligible = plannedDates == null ? 0 : plannedDates.stream()
            .filter(date -> date != null
                && YearMonth.from(date).equals(month)
                && date.getDayOfWeek() != DayOfWeek.SUNDAY)
            .distinct()
            .count();
        BigDecimal remaining = target.subtract(realized).max(BigDecimal.ZERO);
        BigDecimal required = eligible == 0
            ? BigDecimal.ZERO.setScale(2)
            : DecimalPolicy.money(remaining.divide(
                BigDecimal.valueOf(eligible),
                8,
                RoundingMode.HALF_UP
            ));
        BigDecimal progress = target.signum() == 0
            ? new BigDecimal("100.00")
            : realized.multiply(new BigDecimal("100"))
                .divide(target, 2, RoundingMode.HALF_UP);
        GoalStatus status = progress.compareTo(new BigDecimal("100")) >= 0
            ? GoalStatus.ABOVE
            : eligible == 0 ? GoalStatus.BELOW : GoalStatus.ON_TRACK;
        return new GoalProjection(
            DecimalPolicy.money(target),
            DecimalPolicy.money(realized),
            DecimalPolicy.money(remaining),
            (int) eligible,
            required,
            progress,
            status
        );
    }

    @Transactional
    public MonthlyGoal create(
        YearMonth month,
        BigDecimal personal,
        BigDecimal operational,
        WorkloadPeriodicity periodicity,
        long enteredDurationMinutes,
        Set<LocalDate> plannedDates
    ) {
        return create(
            month, personal, operational, periodicity, enteredDurationMinutes,
            plannedDates, defaultVehicleIds()
        );
    }

    @Transactional
    public MonthlyGoal create(
        YearMonth month,
        BigDecimal personal,
        BigDecimal operational,
        WorkloadPeriodicity periodicity,
        long enteredDurationMinutes,
        Set<LocalDate> plannedDates,
        Set<UUID> vehicleIds
    ) {
        if (goals.findByReferenceMonth(month.atDay(1)).isPresent()) {
            throw new IllegalArgumentException("Meta mensal já cadastrada");
        }

        Set<Vehicle> selectedVehicles = activeVehicles(vehicleIds);
        WorkloadCalculation calculation = calculate(
            month,
            periodicity,
            enteredDurationMinutes,
            plannedDates
        );
        MonthlyGoal goal = MonthlyGoal.create(
            month,
            personal,
            operational,
            periodicity,
            enteredDurationMinutes,
            calculation.totalMinutes()
        );
        goal.replaceVehicles(selectedVehicles);
        goal = goals.save(goal);
        savePlannedDays(goal, calculation);
        return goal;
    }

    @Transactional
    public MonthlyGoal update(
        UUID id,
        YearMonth month,
        BigDecimal personal,
        BigDecimal operational,
        WorkloadPeriodicity periodicity,
        long enteredDurationMinutes,
        Set<LocalDate> plannedDates
    ) {
        return update(
            id, month, personal, operational, periodicity, enteredDurationMinutes,
            plannedDates, defaultVehicleIds()
        );
    }

    @Transactional
    public MonthlyGoal update(
        UUID id,
        YearMonth month,
        BigDecimal personal,
        BigDecimal operational,
        WorkloadPeriodicity periodicity,
        long enteredDurationMinutes,
        Set<LocalDate> plannedDates,
        Set<UUID> vehicleIds
    ) {
        MonthlyGoal goal = get(id);
        goals.findByReferenceMonth(month.atDay(1))
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new IllegalArgumentException("Meta mensal já cadastrada");
            });

        Set<Vehicle> selectedVehicles = activeVehicles(vehicleIds);
        WorkloadCalculation calculation = calculate(
            month,
            periodicity,
            enteredDurationMinutes,
            plannedDates
        );
        goal.update(
            month,
            personal,
            operational,
            periodicity,
            enteredDurationMinutes,
            calculation.totalMinutes()
        );
        goal.replaceVehicles(selectedVehicles);
        goals.save(goal);

        days.deleteAllByMonthlyGoalId(id);
        days.flush();
        savePlannedDays(goal, calculation);
        return goal;
    }

    @Transactional
    public MonthlyGoal create(
        YearMonth month,
        BigDecimal personal,
        BigDecimal operational,
        BigDecimal plannedHours,
        Set<LocalDate> plannedDates
    ) {
        return create(
            month, personal, operational, plannedHours, plannedDates,
            defaultVehicleIds()
        );
    }

    @Transactional
    public MonthlyGoal create(
        YearMonth month,
        BigDecimal personal,
        BigDecimal operational,
        BigDecimal plannedHours,
        Set<LocalDate> plannedDates,
        Set<UUID> vehicleIds
    ) {
        if (plannedHours == null || plannedHours.signum() <= 0) {
            throw new IllegalArgumentException("A duração informada deve ser maior que zero.");
        }
        long enteredMinutes = plannedHours.multiply(BigDecimal.valueOf(60))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
        return create(
            month,
            personal,
            operational,
            WorkloadPeriodicity.MONTHLY,
            enteredMinutes,
            plannedDates,
            vehicleIds
        );
    }

    @Transactional(readOnly = true)
    public MonthlyGoal get(UUID id) {
        return goals.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Meta mensal não encontrada"));
    }

    @Transactional(readOnly = true)
    public Optional<MonthlyGoal> find(YearMonth month) {
        return goals.findByReferenceMonth(month.atDay(1));
    }

    @Transactional(readOnly = true)
    public List<PlannedWorkDay> plannedDays(UUID goalId) {
        return days.findAllByMonthlyGoalIdOrderByWorkDateAsc(goalId);
    }

    private Set<UUID> defaultVehicleIds() {
        if (vehicles == null) {
            throw new IllegalArgumentException("Selecione pelo menos um veículo ativo.");
        }
        return vehicles.findByPrimaryVehicleTrueAndStatus(VehicleStatus.ACTIVE)
            .map(vehicle -> Set.of(vehicle.getId()))
            .orElseThrow(() -> new IllegalArgumentException(
                "Selecione pelo menos um veículo ativo."
            ));
    }

    private Set<Vehicle> activeVehicles(Set<UUID> vehicleIds) {
        if (vehicles == null || vehicleIds == null || vehicleIds.isEmpty()) {
            throw new IllegalArgumentException("Selecione pelo menos um veículo ativo.");
        }
        List<Vehicle> found = vehicles.findAllById(vehicleIds);
        if (found.size() != vehicleIds.size()
            || found.stream().anyMatch(vehicle -> vehicle.getStatus() != VehicleStatus.ACTIVE)) {
            throw new IllegalArgumentException("Um dos veículos selecionados não está ativo.");
        }
        return new LinkedHashSet<>(found);
    }

    private WorkloadCalculation calculate(
        YearMonth month,
        WorkloadPeriodicity periodicity,
        long enteredDurationMinutes,
        Set<LocalDate> plannedDates
    ) {
        return workloadPlanner.calculate(
            month,
            periodicity,
            enteredDurationMinutes,
            plannedDates
        );
    }

    private void savePlannedDays(
        MonthlyGoal goal,
        WorkloadCalculation calculation
    ) {
        calculation.days().stream()
            .map(day -> PlannedWorkDay.create(goal, day.date(), day.allocatedMinutes()))
            .forEach(days::save);
    }
}
