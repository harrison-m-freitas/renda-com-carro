package dev.harrison.rendacomcarro.expense;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseCategoryRepository;
import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseRepository;
import dev.harrison.rendacomcarro.expense.web.ExpenseForm;
import dev.harrison.rendacomcarro.security.application.UserTimeZoneService;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties={
    "APP_ADMIN_USERNAME=expense-owner",
    "APP_ADMIN_PASSWORD=expense-owner-credential"
})
@Transactional
class ExpenseWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired VehicleService vehicles;
    @Autowired ExpenseCategoryRepository categories;
    @Autowired ExpenseRepository expenses;
    @Autowired UserTimeZoneService timeZones;

    @Test
    @WithMockUser(username = "expense-owner", roles = "OWNER")
    void expenseListIsAvailableToAuthenticatedOwner() throws Exception {
        mvc.perform(get("/expenses"))
            .andExpect(status().isOk())
            .andExpect(view().name("expenses/list"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @WithMockUser(username = "expense-owner", roles = "OWNER")
    void expenseListRendersVehicleAndCategoryAfterTheReadTransactionCloses() throws Exception {
        var vehicle = createVehicle();
        var category = categories.findAllByActiveTrueOrderByNameAsc().getFirst();

        mvc.perform(post("/expenses")
                .with(csrf())
                .param("vehicleId", vehicle.getId().toString())
                .param("categoryId", category.getId().toString())
                .param("expenseDate", "2026-07-14")
                .param("competenceMonth", "2026-07")
                .param("amount", "125,50")
                .param("classification", "PROFESSIONAL")
                .param("paymentStatus", "PAID")
                .param("paidDate", "2026-07-14"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/expenses"));

        mvc.perform(get("/expenses"))
            .andExpect(status().isOk())
            .andExpect(view().name("expenses/list"))
            .andExpect(content().string(containsString(vehicle.getName())))
            .andExpect(content().string(containsString(category.getName())));
    }

    @Test
    @WithMockUser(username = "expense-owner", roles = "OWNER")
    void expenseFormUsesGuidedLocalizedInputs() throws Exception {
        mvc.perform(get("/expenses/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-guided-form")))
            .andExpect(content().string(containsString("data-draft-type=\"EXPENSE\"")))
            .andExpect(content().string(containsString("data-draft-context-key=\"current\"")))
            .andExpect(content().string(containsString("data-form-step=\"1\"")))
            .andExpect(content().string(containsString("data-form-step=\"2\"")))
            .andExpect(content().string(containsString("data-form-step=\"3\"")))
            .andExpect(content().string(containsString("R$")))
            .andExpect(content().string(containsString("type=\"month\"")))
            .andExpect(content().string(containsString("Selecione um veículo")))
            .andExpect(content().string(containsString("Selecione uma categoria")))
            .andExpect(content().string(containsString("aria-describedby")))
            .andExpect(content().string(containsString("type=\"module\"")));
    }

    @Test
    @WithMockUser(username = "expense-owner", roles = "OWNER")
    void newExpenseUsesPrimaryActiveVehicleAndUserZoneDefaults() throws Exception {
        var archived = createVehicle();
        vehicles.archive(archived.getId());
        var primary = createVehicle();
        vehicles.activate(primary.getId());
        LocalDate expectedDate = timeZones.today("expense-owner");

        mvc.perform(get("/expenses/new"))
            .andExpect(status().isOk())
            .andExpect(model().attribute(
                "expenseForm",
                hasProperty("vehicleId", is(primary.getId()))
            ))
            .andExpect(model().attribute(
                "expenseForm",
                hasProperty("classification", is(ExpenseClassification.PROFESSIONAL))
            ))
            .andExpect(model().attribute(
                "expenseForm",
                hasProperty("allocationMethod", is(AllocationMethod.MILEAGE_RATIO))
            ))
            .andExpect(model().attribute(
                "expenseForm",
                hasProperty("paymentStatus", is(ExpenseForm.PaymentStatus.PAID))
            ))
            .andExpect(model().attribute(
                "expenseForm",
                hasProperty("expenseDate", is(expectedDate))
            ))
            .andExpect(model().attribute(
                "expenseForm",
                hasProperty("paidDate", is(expectedDate))
            ))
            .andExpect(model().attribute(
                "expenseForm",
                hasProperty("competenceMonth", is(YearMonth.from(expectedDate)))
            ))
            .andExpect(content().string(not(containsString(archived.getPlate()))));
    }

    @Test
    @WithMockUser(username = "expense-owner", roles = "OWNER")
    void expenseFormUsesAccessibleCardChoicesAndSchemaTwo() throws Exception {
        mvc.perform(get("/expenses/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-draft-schema-version=\"2\"")))
            .andExpect(content().string(containsString("<fieldset")))
            .andExpect(content().string(containsString("name=\"classification\"")))
            .andExpect(content().string(containsString("type=\"radio\"")))
            .andExpect(content().string(not(containsString("<select class=\"form-select\" id=\"classification\""))))
            .andExpect(content().string(containsString("name=\"paymentStatus\"")))
            .andExpect(content().string(containsString("data-allocation-preview")))
            .andExpect(content().string(containsString("<details")))
            .andExpect(content().string(containsString("Mês de referência")))
            .andExpect(content().string(containsString("data-expense-error-summary")))
            .andExpect(content().string(containsString("role=\"status\"")))
            .andExpect(content().string(containsString("data-natural-percentage-input")));
    }

    @Test
    @WithMockUser(username = "expense-owner", roles = "OWNER")
    void incompatibleAllocationFieldsAreDiscardedForProfessionalExpense() throws Exception {
        var vehicle = createVehicle();
        var category = categories.findAllByActiveTrueOrderByNameAsc().getFirst();

        mvc.perform(post("/expenses")
                .with(csrf())
                .param("vehicleId", vehicle.getId().toString())
                .param("categoryId", category.getId().toString())
                .param("expenseDate", LocalDate.now().toString())
                .param("competenceMonth", "2026-07")
                .param("amount", "100,00")
                .param("classification", "PROFESSIONAL")
                .param("paymentStatus", "PAID")
                .param("paidDate", LocalDate.now().toString())
                .param("allocationMethod", "MANUAL_PERCENTAGE")
                .param("professionalPercentagePercent", "50")
                .param("professionalFixedAmount", "50,00")
                .param("adjustmentReason", "Valor residual enviado pelo navegador"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/expenses"));

        var created = expenses.findAll().stream()
            .filter(expense -> expense.getVehicle().getId().equals(vehicle.getId()))
            .findFirst()
            .orElseThrow();

        assertTrue(created.getClassification().name().equals("PROFESSIONAL"));
        assertNull(created.getAllocationMethod());
        assertNull(created.getProfessionalPercentage());
        assertNull(created.getProfessionalFixedAmount());
        assertNull(created.getAdjustmentReason());
        assertTrue(created.getCompetenceDate().equals(LocalDate.of(2026, 7, 1)));
    }

    @Test
    @WithMockUser(username = "expense-owner", roles = "OWNER")
    void mixedPercentageIsConvertedFromZeroToOneHundredIntoRatio() throws Exception {
        var vehicle = createVehicle();
        var category = categories.findAllByActiveTrueOrderByNameAsc().getFirst();

        mvc.perform(post("/expenses")
                .with(csrf())
                .param("vehicleId", vehicle.getId().toString())
                .param("categoryId", category.getId().toString())
                .param("expenseDate", "2026-07-13")
                .param("competenceMonth", "2026-07")
                .param("amount", "120,50")
                .param("classification", "MIXED")
                .param("paymentStatus", "PAID")
                .param("paidDate", "2026-07-13")
                .param("allocationMethod", "MANUAL_PERCENTAGE")
                .param("professionalPercentagePercent", "75")
                .param("adjustmentReason", "Rateio informado pelo proprietário"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/expenses"));

        var created = expenses.findAll().stream()
            .filter(expense -> expense.getVehicle().getId().equals(vehicle.getId()))
            .findFirst()
            .orElseThrow();
        org.assertj.core.api.Assertions.assertThat(created.getProfessionalPercentage())
            .isEqualByComparingTo("0.7500");
        org.assertj.core.api.Assertions.assertThat(created.getCompetenceDate())
            .isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    @WithMockUser(username = "expense-owner", roles = "OWNER")
    void allocationPreviewReturnsInsufficientDataWithoutInventingPercentage() throws Exception {
        var vehicle = createVehicle();

        mvc.perform(get("/expenses/allocation-preview")
                .param("vehicleId", vehicle.getId().toString())
                .param("competenceMonth", "2038-07"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INSUFFICIENT_DATA"))
            .andExpect(jsonPath("$.professionalPercentage").doesNotExist())
            .andExpect(jsonPath("$.provisional").value(true));
    }

    private dev.harrison.rendacomcarro.vehicle.domain.Vehicle createVehicle() {
        String plate = "E" + UUID.randomUUID().toString().replace("-", "")
            .substring(0, 6).toUpperCase();
        return vehicles.create(new VehicleService.CreateVehicleCommand(
            "Veículo de teste do gasto", "Toyota", "Etios", 2018, plate,
            FuelType.FLEX, new BigDecimal("10000.0"), new BigDecimal("35000.00")
        ));
    }
}
