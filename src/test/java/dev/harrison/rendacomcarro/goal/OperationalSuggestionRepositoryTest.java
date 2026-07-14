package dev.harrison.rendacomcarro.goal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.harrison.rendacomcarro.expense.application.ExpenseService;
import dev.harrison.rendacomcarro.expense.domain.Expense;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseCategoryRepository;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseRepository;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseSuggestionProjection;
import dev.harrison.rendacomcarro.finance.application.FinancialObligationService;
import dev.harrison.rendacomcarro.finance.domain.FinancialObligation;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.domain.ObligationType;
import dev.harrison.rendacomcarro.finance.infrastructure.FinancialObligationRepository;
import dev.harrison.rendacomcarro.finance.infrastructure.InstallmentSuggestionProjection;
import dev.harrison.rendacomcarro.finance.infrastructure.ObligationInstallmentRepository;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=suggestion-repository-owner",
    "APP_ADMIN_PASSWORD=suggestion-repository-password"
})
@Transactional
class OperationalSuggestionRepositoryTest extends PostgresIntegrationTest {
    @Autowired ExpenseService expenseService;
    @Autowired ExpenseRepository expenses;
    @Autowired ExpenseCategoryRepository categories;
    @Autowired FinancialObligationService obligationService;
    @Autowired FinancialObligationRepository obligations;
    @Autowired ObligationInstallmentRepository installments;
    @Autowired VehicleService vehicleService;

    private Vehicle selectedVehicle;
    private Vehicle unselectedVehicle;

    @BeforeEach
    void setUpVehicles() {
        selectedVehicle = vehicle("Selecionado");
        unselectedVehicle = vehicle("Não selecionado");
    }

    @Test
    void expenseCandidatesIncludeSharedAndOldUnpaidButExcludeUnselectedAndOldPaid() {
        Expense sharedProfessionalJuly = expense(
            null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), "100.00"
        );
        Expense selectedVehicleJuly = expense(
            selectedVehicle, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), "200.00"
        );
        Expense unselectedVehicleJuly = expense(
            unselectedVehicle, LocalDate.of(2026, 7, 1), null, "300.00"
        );
        Expense oldUnpaidProfessional = expense(
            selectedVehicle, LocalDate.of(2026, 6, 1), null, "400.00"
        );
        Expense oldPaidProfessional = expense(
            selectedVehicle, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15), "500.00"
        );

        List<ExpenseSuggestionProjection> result = expenses.findSuggestionCandidates(
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 31),
            Set.of(selectedVehicle.getId())
        );

        assertThat(result).extracting(ExpenseSuggestionProjection::getId)
            .containsExactlyInAnyOrder(
                sharedProfessionalJuly.getId(),
                selectedVehicleJuly.getId(),
                oldUnpaidProfessional.getId()
            )
            .doesNotContain(unselectedVehicleJuly.getId(), oldPaidProfessional.getId());
    }

    @Test
    void obligationCandidatesContainRemainingStructuredAndActiveFlexibleAmountsOnly() {
        FinancialObligation current = structured(
            selectedVehicle, "Atual", "1400.00", LocalDate.of(2026, 7, 5), 2
        );
        obligationService.pay(
            current.getId(), LocalDate.of(2026, 7, 10),
            new BigDecimal("250.00"), BigDecimal.ZERO, BigDecimal.ZERO,
            "CURRENT-PARTIAL", null
        );
        UUID currentPartial = obligationService.schedule(current.getId()).getFirst().getId();

        FinancialObligation overdue = structured(
            selectedVehicle, "Atrasada", "350.00", LocalDate.of(2026, 6, 5), 1
        );
        UUID overduePending = obligationService.schedule(overdue.getId()).getFirst().getId();

        FinancialObligation paid = structured(
            selectedVehicle, "Paga", "100.00", LocalDate.of(2026, 5, 5), 1
        );
        obligationService.pay(
            paid.getId(), LocalDate.of(2026, 5, 5),
            new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO,
            "PAID", null
        );
        UUID paidInstallment = obligationService.schedule(paid.getId()).getFirst().getId();

        FinancialObligation unselected = structured(
            unselectedVehicle, "Outro veículo", "200.00", LocalDate.of(2026, 6, 5), 1
        );
        UUID unselectedInstallment = obligationService.schedule(unselected.getId()).getFirst().getId();

        FinancialObligation activeVehicleFlexible = flexible(
            selectedVehicle, "Flexível do veículo", "500.00"
        );
        FinancialObligation personalFlexible = flexible(
            null, "Flexível pessoal", "600.00"
        );

        List<InstallmentSuggestionProjection> structured = installments
            .findSuggestionCandidates(
                Set.of(selectedVehicle.getId()),
                LocalDate.of(2026, 7, 31)
            );
        List<FinancialObligation> flexible = obligations
            .findActiveFlexibleTargets(Set.of(selectedVehicle.getId()));

        assertThat(structured).extracting(InstallmentSuggestionProjection::getInstallmentId)
            .containsExactlyInAnyOrder(currentPartial, overduePending)
            .doesNotContain(paidInstallment, unselectedInstallment);
        assertThat(flexible).extracting(FinancialObligation::getId)
            .containsExactly(activeVehicleFlexible.getId())
            .doesNotContain(personalFlexible.getId());
    }

    private Expense expense(
        Vehicle vehicle,
        LocalDate competence,
        LocalDate paidDate,
        String amount
    ) {
        var category = categories.findAllByActiveTrueOrderByNameAsc().getFirst();
        return expenseService.create(new ExpenseService.CreateExpenseCommand(
            vehicle == null ? null : vehicle.getId(),
            null,
            null,
            category.getId(),
            competence,
            competence,
            paidDate,
            new BigDecimal(amount),
            ExpenseClassification.PROFESSIONAL,
            null,
            null,
            null,
            null,
            null
        ));
    }

    private FinancialObligation structured(
        Vehicle vehicle,
        String creditor,
        String principal,
        LocalDate firstDue,
        int months
    ) {
        return obligationService.create(new FinancialObligationService.CreateCommand(
            vehicle.getId(),
            ObligationType.BANK_FINANCING,
            ObligationMode.STRUCTURED,
            creditor,
            new BigDecimal(principal),
            BigDecimal.ZERO,
            firstDue.minusMonths(1),
            firstDue,
            months,
            null,
            null,
            null
        ));
    }

    private FinancialObligation flexible(Vehicle vehicle, String creditor, String target) {
        return obligationService.create(new FinancialObligationService.CreateCommand(
            vehicle == null ? null : vehicle.getId(),
            ObligationType.FAMILY_LOAN,
            ObligationMode.FLEXIBLE,
            creditor,
            new BigDecimal("5000.00"),
            BigDecimal.ZERO,
            LocalDate.of(2026, 1, 1),
            null,
            null,
            null,
            new BigDecimal(target),
            null
        ));
    }

    private Vehicle vehicle(String name) {
        String plate = "R" + UUID.randomUUID().toString().replace("-", "")
            .substring(0, 6).toUpperCase();
        return vehicleService.create(new VehicleService.CreateVehicleCommand(
            name, "Toyota", "Etios", 2018, plate,
            FuelType.FLEX, new BigDecimal("10000.0"), new BigDecimal("35000.00")
        ));
    }
}
