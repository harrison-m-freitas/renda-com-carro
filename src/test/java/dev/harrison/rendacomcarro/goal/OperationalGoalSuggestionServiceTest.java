package dev.harrison.rendacomcarro.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.harrison.rendacomcarro.expense.application.ExpenseAllocationService;
import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseRepository;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseSuggestionProjection;
import dev.harrison.rendacomcarro.expense.infrastructure.MonthlyOdometerClosingRepository;
import dev.harrison.rendacomcarro.finance.domain.FinancialObligation;
import dev.harrison.rendacomcarro.finance.infrastructure.FinancialObligationRepository;
import dev.harrison.rendacomcarro.finance.infrastructure.InstallmentSuggestionProjection;
import dev.harrison.rendacomcarro.finance.infrastructure.ObligationInstallmentRepository;
import dev.harrison.rendacomcarro.goal.application.GoalMonthLabelFormatter;
import dev.harrison.rendacomcarro.goal.application.OperationalGoalSuggestion;
import dev.harrison.rendacomcarro.goal.application.OperationalGoalSuggestionItem;
import dev.harrison.rendacomcarro.goal.application.OperationalGoalSuggestionService;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OperationalGoalSuggestionServiceTest {
    private final ExpenseRepository expenses = mock(ExpenseRepository.class);
    private final ObligationInstallmentRepository installments =
        mock(ObligationInstallmentRepository.class);
    private final FinancialObligationRepository obligations =
        mock(FinancialObligationRepository.class);
    private final MonthlyOdometerClosingRepository closings =
        mock(MonthlyOdometerClosingRepository.class);

    private OperationalGoalSuggestionService service;
    private UUID selectedVehicleId;
    private UUID sharedExpenseId;
    private UUID manualPercentageExpenseId;
    private UUID fixedExpenseId;
    private UUID professionalExpenseId;
    private UUID overdueExpenseId;
    private UUID personalExpenseId;
    private UUID mileageExpenseId;
    private UUID currentInstallmentId;
    private UUID overdueInstallmentId;
    private UUID flexibleObligationId;

    @BeforeEach
    void setUp() {
        service = new OperationalGoalSuggestionService(
            expenses,
            installments,
            obligations,
            closings,
            new ExpenseAllocationService(),
            new GoalMonthLabelFormatter()
        );
        selectedVehicleId = UUID.randomUUID();
        sharedExpenseId = UUID.randomUUID();
        manualPercentageExpenseId = UUID.randomUUID();
        fixedExpenseId = UUID.randomUUID();
        professionalExpenseId = UUID.randomUUID();
        overdueExpenseId = UUID.randomUUID();
        personalExpenseId = UUID.randomUUID();
        mileageExpenseId = UUID.randomUUID();
        currentInstallmentId = UUID.randomUUID();
        overdueInstallmentId = UUID.randomUUID();
        flexibleObligationId = UUID.randomUUID();

        List<ExpenseSuggestionProjection> expenseCandidates = List.of(
            expense(sharedExpenseId, null, "500.00", ExpenseClassification.PROFESSIONAL,
                null, null, null, LocalDate.of(2026, 7, 1)),
            expense(manualPercentageExpenseId, selectedVehicleId, "600.00",
                ExpenseClassification.MIXED, AllocationMethod.MANUAL_PERCENTAGE,
                "0.5000", null, LocalDate.of(2026, 7, 1)),
            expense(fixedExpenseId, selectedVehicleId, "200.00",
                ExpenseClassification.MIXED, AllocationMethod.FIXED_AMOUNT,
                null, "80.00", LocalDate.of(2026, 7, 1)),
            expense(professionalExpenseId, selectedVehicleId, "50.00",
                ExpenseClassification.PROFESSIONAL, null, null, null,
                LocalDate.of(2026, 7, 1)),
            expense(overdueExpenseId, selectedVehicleId, "280.00",
                ExpenseClassification.PROFESSIONAL, null, null, null,
                LocalDate.of(2026, 6, 1)),
            expense(personalExpenseId, selectedVehicleId, "999.00",
                ExpenseClassification.PERSONAL, null, null, null,
                LocalDate.of(2026, 7, 1)),
            expense(mileageExpenseId, selectedVehicleId, "100.00",
                ExpenseClassification.MIXED, AllocationMethod.MILEAGE_RATIO,
                null, null, LocalDate.of(2026, 7, 1))
        );
        when(expenses.findSuggestionCandidates(
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 31),
            selectedVehicleId
        )).thenReturn(expenseCandidates);
        when(closings
            .findAllByVehicleIdAndReferenceMonthLessThanEqualOrderByReferenceMonthDesc(
                selectedVehicleId, LocalDate.of(2026, 7, 1)
            )).thenReturn(List.of());
        List<InstallmentSuggestionProjection> installmentCandidates = List.of(
            installment(currentInstallmentId, "700.00", "0.00", LocalDate.of(2026, 7, 5)),
            installment(overdueInstallmentId, "600.00", "250.00", LocalDate.of(2026, 6, 5))
        );
        when(installments.findSuggestionCandidates(
            selectedVehicleId, LocalDate.of(2026, 7, 31)
        )).thenReturn(installmentCandidates);
        FinancialObligation flexible = flexibleObligation("500.00", "500.00");
        when(obligations.findActiveFlexibleTargets(selectedVehicleId))
            .thenReturn(List.of(flexible));
    }

    @Test
    void consolidatesCurrentOverdueSharedMixedAndVehicleObligationCosts() {
        OperationalGoalSuggestion result = service.suggest(
            YearMonth.of(2026, 7), selectedVehicleId
        );

        assertThat(result.currentExpenses()).isEqualByComparingTo("930.00");
        assertThat(result.overdueProfessionalExpenses()).isEqualByComparingTo("280.00");
        assertThat(result.currentVehicleObligations()).isEqualByComparingTo("1200.00");
        assertThat(result.overdueVehicleObligations()).isEqualByComparingTo("350.00");
        assertThat(result.professionalCostsTotal()).isEqualByComparingTo("2760.00");
        assertThat(result.items()).extracting(OperationalGoalSuggestionItem::sourceId)
            .contains(
                sharedExpenseId,
                manualPercentageExpenseId,
                fixedExpenseId,
                professionalExpenseId,
                currentInstallmentId,
                overdueInstallmentId,
                flexibleObligationId
            )
            .doesNotContain(personalExpenseId, mileageExpenseId);
    }


    @Test
    void capsFlexibleMonthlyTargetAtTheRemainingBalance() {
        FinancialObligation capped = flexibleObligation("700.00", "300.00");
        when(obligations.findActiveFlexibleTargets(selectedVehicleId))
            .thenReturn(List.of(capped));

        OperationalGoalSuggestion result = service.suggest(
            YearMonth.of(2026, 7), selectedVehicleId
        );

        assertThat(result.currentVehicleObligations()).isEqualByComparingTo("1000.00");
        assertThat(result.items())
            .filteredOn(item -> item.sourceId().equals(flexibleObligationId))
            .extracting(OperationalGoalSuggestionItem::amount)
            .containsExactly(new BigDecimal("300.00"));
    }

    @Test
    void excludesMileageAllocatedExpenseWithoutSnapshotAndExplainsWhy() {
        OperationalGoalSuggestion result = service.suggest(
            YearMonth.of(2026, 7), selectedVehicleId
        );

        assertThat(result.ignoredItems()).extracting(OperationalGoalSuggestionItem::sourceId)
            .containsExactly(mileageExpenseId);
        assertThat(result.warnings()).contains(
            "1 gasto misto não foi incluído porque não há percentual profissional disponível."
        );
    }

    private ExpenseSuggestionProjection expense(
        UUID id,
        UUID vehicleId,
        String amount,
        ExpenseClassification classification,
        AllocationMethod method,
        String percentage,
        String fixed,
        LocalDate competence
    ) {
        ExpenseSuggestionProjection projection = mock(ExpenseSuggestionProjection.class);
        when(projection.getId()).thenReturn(id);
        when(projection.getVehicleId()).thenReturn(vehicleId);
        when(projection.getVehicleName()).thenReturn(
            vehicleId == null ? null : "Veículo selecionado"
        );
        when(projection.getCategoryId()).thenReturn(UUID.randomUUID());
        when(projection.getCategoryName()).thenReturn("Categoria");
        when(projection.getCompetenceDate()).thenReturn(competence);
        when(projection.getPaidDate()).thenReturn(null);
        when(projection.getAmount()).thenReturn(new BigDecimal(amount));
        when(projection.getClassification()).thenReturn(classification);
        when(projection.getAllocationMethod()).thenReturn(method);
        when(projection.getProfessionalPercentage()).thenReturn(
            percentage == null ? null : new BigDecimal(percentage)
        );
        when(projection.getProfessionalFixedAmount()).thenReturn(
            fixed == null ? null : new BigDecimal(fixed)
        );
        return projection;
    }

    private InstallmentSuggestionProjection installment(
        UUID id,
        String expected,
        String paid,
        LocalDate dueDate
    ) {
        InstallmentSuggestionProjection projection = mock(InstallmentSuggestionProjection.class);
        when(projection.getInstallmentId()).thenReturn(id);
        when(projection.getObligationId()).thenReturn(UUID.randomUUID());
        when(projection.getVehicleId()).thenReturn(selectedVehicleId);
        when(projection.getVehicleName()).thenReturn("Veículo selecionado");
        when(projection.getCreditor()).thenReturn("Banco");
        when(projection.getDueDate()).thenReturn(dueDate);
        when(projection.getExpectedAmount()).thenReturn(new BigDecimal(expected));
        when(projection.getPaidAmount()).thenReturn(new BigDecimal(paid));
        return projection;
    }

    private FinancialObligation flexibleObligation(String target, String balance) {
        FinancialObligation obligation = mock(FinancialObligation.class);
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getId()).thenReturn(selectedVehicleId);
        when(vehicle.getName()).thenReturn("Veículo selecionado");
        when(obligation.getId()).thenReturn(flexibleObligationId);
        when(obligation.getVehicle()).thenReturn(vehicle);
        when(obligation.getCreditor()).thenReturn("Família");
        when(obligation.getMonthlyTarget()).thenReturn(new BigDecimal(target));
        when(obligation.getCurrentBalance()).thenReturn(new BigDecimal(balance));
        return obligation;
    }
}
