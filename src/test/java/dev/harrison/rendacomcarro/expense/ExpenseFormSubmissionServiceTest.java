package dev.harrison.rendacomcarro.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.application.FormDraftService.SaveDraftCommand;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.expense.application.ExpenseFormSubmissionService;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseCategoryRepository;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseRepository;
import dev.harrison.rendacomcarro.expense.web.ExpenseForm;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    private ExpenseForm validForm(UUID vehicleId, UUID categoryId) {
        ExpenseForm form = new ExpenseForm();
        form.setVehicleId(vehicleId);
        form.setCategoryId(categoryId);
        form.setExpenseDate(LocalDate.of(2026, 7, 13));
        form.setCompetenceMonth(YearMonth.of(2026, 7));
        form.setAmount(new BigDecimal("120.50"));
        form.setClassification(ExpenseClassification.PROFESSIONAL);
        return form;
    }

    private void seedDraft(UUID vehicleId, UUID categoryId) {
        drafts.save("expense-submission-owner", new SaveDraftCommand(
            FormDraftType.EXPENSE,
            "current",
            1,
            2,
            null,
            mapper.createObjectNode()
                .put("vehicleId", vehicleId.toString())
                .put("categoryId", categoryId.toString())
                .put("expenseDate", "2026-07-13")
                .put("competenceMonth", "2026-07")
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
