package dev.harrison.rendacomcarro.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.application.FormDraftService.SaveDraftCommand;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.expense.application.ExpenseFormSubmissionService;
import dev.harrison.rendacomcarro.expense.application.ExpenseFormValidationException;
import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseCategoryRepository;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseRepository;
import dev.harrison.rendacomcarro.expense.web.ExpenseForm;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=expense-submission-owner",
    "APP_ADMIN_PASSWORD=expense-submission-password"
})
@Transactional
class ExpenseFormSubmissionServiceTest extends PostgresIntegrationTest {
    @Autowired ExpenseFormSubmissionService submissions;
    @Autowired FormDraftService drafts;
    @Autowired ExpenseRepository expenses;
    @Autowired ExpenseCategoryRepository categories;
    @Autowired VehicleService vehicles;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void createsExpenseAndDeletesMatchingDraftAtomically() {
        var vehicle = createVehicle();
        var category = categories.findAllByActiveTrueOrderByNameAsc().getFirst();
        seedDraft(vehicle.getId(), category.getId());

        ExpenseForm form = validForm(vehicle.getId(), category.getId());
        submissions.submit("expense-submission-owner", form);

        assertThat(expenses.count()).isEqualTo(1);
        assertThat(drafts.find(
            "expense-submission-owner", FormDraftType.EXPENSE, "current"
        )).isEmpty();
    }

    @Test
    void failedFinalSubmissionPreservesDraft() {
        var vehicle = createVehicle();
        UUID missingCategory = UUID.randomUUID();
        seedDraft(vehicle.getId(), missingCategory);

        ExpenseForm invalid = validForm(vehicle.getId(), missingCategory);

        assertThatThrownBy(() -> submissions.submit("expense-submission-owner", invalid))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(drafts.find(
            "expense-submission-owner", FormDraftType.EXPENSE, "current"
        )).isPresent();
    }

    @Test
    void pendingPaymentClearsForgedPaidDate() {
        var vehicle = createVehicle();
        var category = categories.findAllByActiveTrueOrderByNameAsc().getFirst();
        ExpenseForm form = validForm(vehicle.getId(), category.getId());
        form.setPaymentStatus(ExpenseForm.PaymentStatus.PENDING);
        form.setPaidDate(LocalDate.of(2026, 7, 10));

        var created = submissions.submit("expense-submission-owner", form);

        assertThat(created.getPaidDate()).isNull();
    }

    @Test
    void paidPaymentRequiresDate() {
        var vehicle = createVehicle();
        var category = categories.findAllByActiveTrueOrderByNameAsc().getFirst();
        ExpenseForm form = validForm(vehicle.getId(), category.getId());
        form.setPaymentStatus(ExpenseForm.PaymentStatus.PAID);
        form.setPaidDate(null);

        assertThatThrownBy(() -> submissions.submit("expense-submission-owner", form))
            .isInstanceOf(ExpenseFormValidationException.class)
            .satisfies(exception -> assertThat(((ExpenseFormValidationException) exception).field())
                .isEqualTo("paidDate"));
    }

    @Test
    void mixedPercentageMustBeStrictlyBetweenZeroAndOneHundred() {
        var vehicle = createVehicle();
        var category = categories.findAllByActiveTrueOrderByNameAsc().getFirst();
        ExpenseForm form = validForm(vehicle.getId(), category.getId());
        form.setClassification(ExpenseClassification.MIXED);
        form.setAllocationMethod(AllocationMethod.MANUAL_PERCENTAGE);
        form.setProfessionalPercentagePercent(new BigDecimal("100"));
        form.setAdjustmentReason("Rateio manual comprovado");

        assertThatThrownBy(() -> submissions.submit("expense-submission-owner", form))
            .isInstanceOf(ExpenseFormValidationException.class)
            .hasMessage("Para 100%, classifique o gasto como Profissional.");
    }

    @Test
    void mixedFixedAmountMustBeStrictlyBelowTotal() {
        var vehicle = createVehicle();
        var category = categories.findAllByActiveTrueOrderByNameAsc().getFirst();
        ExpenseForm form = validForm(vehicle.getId(), category.getId());
        form.setClassification(ExpenseClassification.MIXED);
        form.setAllocationMethod(AllocationMethod.FIXED_AMOUNT);
        form.setProfessionalFixedAmount(new BigDecimal("120.50"));
        form.setAdjustmentReason("Rateio manual comprovado");

        assertThatThrownBy(() -> submissions.submit("expense-submission-owner", form))
            .isInstanceOf(ExpenseFormValidationException.class)
            .hasMessage("Para atribuir todo o valor à operação, classifique o gasto como Profissional.");
    }

    @Test
    void archivedVehicleAndInactiveCategoryAreRejected() {
        var archived = createVehicle();
        createVehicle();
        var category = categories.findAllByActiveTrueOrderByNameAsc().getFirst();
        vehicles.archive(archived.getId());

        assertThatThrownBy(() -> submissions.submit(
            "expense-submission-owner", validForm(archived.getId(), category.getId())
        )).hasMessage("Veículo ativo não encontrado");

        var active = createVehicle();
        jdbc.update("update expense_category set active = false where id = ?", category.getId());
        entityManager.flush();
        entityManager.clear();
        assertThatThrownBy(() -> submissions.submit(
            "expense-submission-owner", validForm(active.getId(), category.getId())
        )).hasMessage("Categoria ativa não encontrada");
    }

    private ExpenseForm validForm(UUID vehicleId, UUID categoryId) {
        ExpenseForm form = new ExpenseForm();
        form.setVehicleId(vehicleId);
        form.setCategoryId(categoryId);
        form.setExpenseDate(LocalDate.of(2026, 7, 13));
        form.setCompetenceMonth(YearMonth.of(2026, 7));
        form.setAmount(new BigDecimal("120.50"));
        form.setClassification(ExpenseClassification.PROFESSIONAL);
        form.setPaymentStatus(ExpenseForm.PaymentStatus.PAID);
        form.setPaidDate(LocalDate.of(2026, 7, 13));
        return form;
    }

    private void seedDraft(UUID vehicleId, UUID categoryId) {
        drafts.save("expense-submission-owner", new SaveDraftCommand(
            FormDraftType.EXPENSE,
            "current",
            2,
            2,
            null,
            mapper.createObjectNode()
                .put("vehicleId", vehicleId.toString())
                .put("categoryId", categoryId.toString())
                .put("expenseDate", "2026-07-13")
                .put("competenceMonth", "2026-07")
                .put("paymentStatus", "PAID")
                .put("paidDate", "2026-07-13")
                .put("amount", "120,50")
                .put("classification", "PROFESSIONAL"),
            false
        ));
    }

    private dev.harrison.rendacomcarro.vehicle.domain.Vehicle createVehicle() {
        String plate = "E" + UUID.randomUUID().toString().replace("-", "")
            .substring(0, 6).toUpperCase();
        return vehicles.create(new VehicleService.CreateVehicleCommand(
            "Veículo gasto", "Toyota", "Etios", 2018, plate,
            FuelType.FLEX, new BigDecimal("10000.0"), new BigDecimal("35000.00")
        ));
    }
}
