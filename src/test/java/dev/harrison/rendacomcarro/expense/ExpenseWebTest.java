package dev.harrison.rendacomcarro.expense;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseCategoryRepository;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseRepository;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties={
    "APP_ADMIN_USERNAME=expense-owner",
    "APP_ADMIN_PASSWORD=expense-owner-credential"
})
class ExpenseWebTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired VehicleService vehicles;
    @Autowired ExpenseCategoryRepository categories;
    @Autowired ExpenseRepository expenses;

    @Test
    @WithMockUser
    void expenseListIsAvailableToAuthenticatedOwner() throws Exception {
        mvc.perform(get("/expenses"))
            .andExpect(status().isOk())
            .andExpect(view().name("expenses/list"));
    }

    @Test
    @WithMockUser
    void incompatibleAllocationFieldsAreDiscardedForProfessionalExpense() throws Exception {
        var vehicle = vehicles.create(new VehicleService.CreateVehicleCommand(
            "Veículo de teste do gasto",
            "Toyota",
            "Etios",
            2018,
            "EXP1A23",
            FuelType.FLEX,
            new BigDecimal("10000.0"),
            new BigDecimal("35000.00")
        ));
        var category = categories.findAllByActiveTrueOrderByNameAsc().getFirst();

        mvc.perform(post("/expenses")
                .with(csrf())
                .param("vehicleId", vehicle.getId().toString())
                .param("categoryId", category.getId().toString())
                .param("expenseDate", LocalDate.now().toString())
                .param("competenceDate", LocalDate.now().toString())
                .param("amount", "100.00")
                .param("classification", "PROFESSIONAL")
                .param("allocationMethod", "MANUAL_PERCENTAGE")
                .param("professionalPercentage", "0.5000")
                .param("professionalFixedAmount", "50.00")
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
    }
}
