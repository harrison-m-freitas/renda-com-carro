package dev.harrison.rendacomcarro.goal.application;

import dev.harrison.rendacomcarro.expense.application.ExpenseAllocationService;
import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.MonthlyMileageSnapshot;
import dev.harrison.rendacomcarro.expense.domain.MonthlyOdometerClosing;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseRepository;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseSuggestionProjection;
import dev.harrison.rendacomcarro.expense.infrastructure.MonthlyOdometerClosingRepository;
import dev.harrison.rendacomcarro.finance.domain.FinancialObligation;
import dev.harrison.rendacomcarro.finance.infrastructure.FinancialObligationRepository;
import dev.harrison.rendacomcarro.finance.infrastructure.InstallmentSuggestionProjection;
import dev.harrison.rendacomcarro.finance.infrastructure.ObligationInstallmentRepository;
import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationalGoalSuggestionService {
    private final ExpenseRepository expenses;
    private final ObligationInstallmentRepository installments;
    private final FinancialObligationRepository obligations;
    private final MonthlyOdometerClosingRepository closings;
    private final ExpenseAllocationService allocations;
    private final GoalMonthLabelFormatter monthLabels;

    public OperationalGoalSuggestionService(
        ExpenseRepository expenses,
        ObligationInstallmentRepository installments,
        FinancialObligationRepository obligations,
        MonthlyOdometerClosingRepository closings,
        ExpenseAllocationService allocations,
        GoalMonthLabelFormatter monthLabels
    ) {
        this.expenses = expenses;
        this.installments = installments;
        this.obligations = obligations;
        this.closings = closings;
        this.allocations = allocations;
        this.monthLabels = monthLabels;
    }

    @Transactional(readOnly = true)
    public OperationalGoalSuggestion suggest(YearMonth month, Set<UUID> vehicleIds) {
        if (month == null || vehicleIds == null || vehicleIds.isEmpty()) {
            throw new IllegalArgumentException("Mês e veículos são obrigatórios");
        }

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        List<OperationalGoalSuggestionItem> items = new ArrayList<>();
        List<OperationalGoalSuggestionItem> ignoredItems = new ArrayList<>();
        List<MonthlyOdometerClosing> mileageClosings = closings
            .findAllByVehicleIdInAndReferenceMonthLessThanEqualOrderByVehicleIdAscReferenceMonthDesc(
                vehicleIds,
                monthStart
            );

        BigDecimal currentExpenses = BigDecimal.ZERO;
        BigDecimal overdueExpenses = BigDecimal.ZERO;
        int missingMileageCount = 0;

        for (ExpenseSuggestionProjection expense : expenses.findSuggestionCandidates(
            monthStart,
            monthEnd,
            vehicleIds
        )) {
            if (expense.getClassification()
                == dev.harrison.rendacomcarro.expense.domain.ExpenseClassification.PERSONAL) {
                continue;
            }

            MonthlyMileageSnapshot mileage = null;
            if (expense.getAllocationMethod() == AllocationMethod.MILEAGE_RATIO) {
                mileage = mileageSnapshot(expense, mileageClosings);
                if (mileage == null) {
                    missingMileageCount++;
                    ignoredItems.add(new OperationalGoalSuggestionItem(
                        SuggestionItemType.EXPENSE,
                        expense.getId(),
                        expense.getVehicleId(),
                        expense.getVehicleName(),
                        expense.getCategoryName(),
                        expense.getAmount(),
                        expense.getCompetenceDate().isBefore(monthStart),
                        "Sem fechamento de quilometragem disponível para o rateio."
                    ));
                    continue;
                }
            }

            BigDecimal professional = allocations.allocate(
                expense.getAmount(),
                expense.getClassification(),
                expense.getAllocationMethod(),
                expense.getProfessionalPercentage(),
                expense.getProfessionalFixedAmount(),
                mileage
            ).professionalAmount();
            if (professional.signum() == 0) {
                continue;
            }

            boolean overdue = expense.getCompetenceDate().isBefore(monthStart);
            if (overdue) {
                overdueExpenses = overdueExpenses.add(professional);
            } else {
                currentExpenses = currentExpenses.add(professional);
            }
            items.add(new OperationalGoalSuggestionItem(
                SuggestionItemType.EXPENSE,
                expense.getId(),
                expense.getVehicleId(),
                expense.getVehicleName(),
                expense.getCategoryName(),
                professional,
                overdue,
                expenseExplanation(expense)
            ));
        }

        BigDecimal currentObligations = BigDecimal.ZERO;
        BigDecimal overdueObligations = BigDecimal.ZERO;
        for (InstallmentSuggestionProjection installment : installments
            .findSuggestionCandidates(vehicleIds, monthEnd)) {
            BigDecimal remaining = DecimalPolicy.money(
                installment.getExpectedAmount().subtract(installment.getPaidAmount())
                    .max(BigDecimal.ZERO)
            );
            if (remaining.signum() == 0) {
                continue;
            }
            boolean overdue = installment.getDueDate().isBefore(monthStart);
            if (overdue) {
                overdueObligations = overdueObligations.add(remaining);
            } else {
                currentObligations = currentObligations.add(remaining);
            }
            items.add(new OperationalGoalSuggestionItem(
                SuggestionItemType.INSTALLMENT,
                installment.getInstallmentId(),
                installment.getVehicleId(),
                installment.getVehicleName(),
                installment.getCreditor(),
                remaining,
                overdue,
                overdue
                    ? "Saldo integral de parcela vencida do veículo."
                    : "Saldo da parcela do veículo com vencimento no mês."
            ));
        }

        for (FinancialObligation obligation : obligations.findActiveFlexibleTargets(vehicleIds)) {
            BigDecimal target = DecimalPolicy.money(obligation.getMonthlyTarget());
            if (target.signum() == 0) {
                continue;
            }
            currentObligations = currentObligations.add(target);
            items.add(new OperationalGoalSuggestionItem(
                SuggestionItemType.FLEXIBLE_OBLIGATION,
                obligation.getId(),
                obligation.getVehicle().getId(),
                obligation.getVehicle().getName(),
                obligation.getCreditor(),
                target,
                false,
                "Meta mensal da obrigação flexível vinculada ao veículo."
            ));
        }

        currentExpenses = DecimalPolicy.money(currentExpenses);
        overdueExpenses = DecimalPolicy.money(overdueExpenses);
        currentObligations = DecimalPolicy.money(currentObligations);
        overdueObligations = DecimalPolicy.money(overdueObligations);
        BigDecimal total = DecimalPolicy.money(Stream.of(
            currentExpenses,
            overdueExpenses,
            currentObligations,
            overdueObligations
        ).reduce(BigDecimal.ZERO, BigDecimal::add));

        List<String> warnings = missingMileageCount == 0
            ? List.of()
            : List.of(missingMileageCount == 1
                ? "1 gasto misto não foi incluído porque não há percentual profissional disponível."
                : missingMileageCount
                    + " gastos mistos não foram incluídos porque não há percentual profissional disponível.");

        return new OperationalGoalSuggestion(
            month,
            monthLabels.format(month),
            vehicleIds,
            currentExpenses,
            overdueExpenses,
            currentObligations,
            overdueObligations,
            total,
            items,
            ignoredItems,
            warnings
        );
    }

    private MonthlyMileageSnapshot mileageSnapshot(
        ExpenseSuggestionProjection expense,
        List<MonthlyOdometerClosing> closings
    ) {
        if (expense.getVehicleId() == null) {
            return null;
        }
        LocalDate competenceMonth = YearMonth.from(expense.getCompetenceDate()).atDay(1);
        return closings.stream()
            .filter(closing -> closing.getVehicle().getId().equals(expense.getVehicleId()))
            .filter(closing -> !closing.getReferenceMonth().isAfter(competenceMonth))
            .findFirst()
            .map(closing -> new MonthlyMileageSnapshot(
                closing.getTotalKilometers(),
                closing.getProfessionalKilometers()
            ))
            .orElse(null);
    }

    private String expenseExplanation(ExpenseSuggestionProjection expense) {
        if (expense.getClassification()
            == dev.harrison.rendacomcarro.expense.domain.ExpenseClassification.PROFESSIONAL) {
            return "Gasto integral classificado como profissional.";
        }
        return switch (expense.getAllocationMethod()) {
            case MANUAL_PERCENTAGE -> "Parcela profissional calculada pelo percentual informado.";
            case FIXED_AMOUNT -> "Parcela profissional definida por valor fixo.";
            case MILEAGE_RATIO -> "Parcela profissional calculada pelo uso do veículo.";
        };
    }
}
