package dev.harrison.rendacomcarro;

import dev.harrison.rendacomcarro.dashboard.application.DashboardService;
import dev.harrison.rendacomcarro.expense.application.ExpenseService;
import dev.harrison.rendacomcarro.expense.application.MonthlyOdometerClosingService;
import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseCategoryRepository;
import dev.harrison.rendacomcarro.finance.application.FinancialObligationService;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.domain.ObligationType;
import dev.harrison.rendacomcarro.fuel.application.FuelingService;
import dev.harrison.rendacomcarro.goal.application.GoalService;
import dev.harrison.rendacomcarro.operation.application.OperationalDayService;
import dev.harrison.rendacomcarro.operation.application.RevenueService;
import dev.harrison.rendacomcarro.operation.application.ShiftService;
import dev.harrison.rendacomcarro.operation.domain.DataSource;
import dev.harrison.rendacomcarro.operation.domain.Platform;
import dev.harrison.rendacomcarro.operation.domain.RevenueType;
import dev.harrison.rendacomcarro.operation.infrastructure.PlatformRepository;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=acceptance-owner",
    "APP_ADMIN_PASSWORD=acceptance-owner-credential"
})
class AcceptanceFlowTest extends PostgresIntegrationTest {
    private static final LocalDate OPERATION_DATE = LocalDate.of(2035, 7, 16);

    @Autowired private VehicleService vehicles;
    @Autowired private GoalService goals;
    @Autowired private OperationalDayService days;
    @Autowired private ShiftService shifts;
    @Autowired private RevenueService revenues;
    @Autowired private PlatformRepository platforms;
    @Autowired private ExpenseService expenses;
    @Autowired private ExpenseCategoryRepository expenseCategories;
    @Autowired private MonthlyOdometerClosingService mileageClosings;
    @Autowired private FuelingService fuelings;
    @Autowired private FinancialObligationService obligations;
    @Autowired private DashboardService dashboard;
    @Autowired private MockMvc mvc;

    @Test
    void ownerCanExecuteCoreOperationalAndFinancialFlow() throws Exception {
        var loginResult = mvc.perform(post("/login")
                .with(csrf())
                .param("username", "acceptance-owner")
                .param("password", "acceptance-owner-credential"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"))
            .andExpect(SecurityMockMvcResultMatchers.authenticated()
                .withUsername("acceptance-owner"))
            .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        var vehicle = vehicles.create(new VehicleService.CreateVehicleCommand(
            "Carro aceite",
            "Honda",
            "Fit",
            2016,
            "ACC1A23",
            FuelType.FLEX,
            new BigDecimal("50000.0"),
            new BigDecimal("45000.00")
        ));
        vehicles.activateAsPrimary(vehicle.getId());

        goals.create(
            YearMonth.from(OPERATION_DATE),
            new BigDecimal("2100.00"),
            new BigDecimal("5000.00"),
            new BigDecimal("160.00"),
            Set.of(OPERATION_DATE)
        );

        var day = days.openDay(
            OPERATION_DATE,
            vehicle.getId(),
            new BigDecimal("200.00"),
            new BigDecimal("50000.0")
        );
        Platform uber = platforms.findByCode("UBER").orElseThrow();
        Platform ninetyNine = platforms.findByCode("99").orElseThrow();
        var shift = shifts.openShift(
            day.getId(),
            LocalDateTime.of(2035, 7, 16, 8, 0),
            new BigDecimal("50000.0"),
            "Centro",
            Set.of(uber.getId(), ninetyNine.getId())
        );
        assertThat(shift.getPlatforms())
            .extracting(Platform::getCode)
            .containsExactlyInAnyOrder("UBER", "99");

        revenues.create(new RevenueService.CreateRevenueCommand(
            shift.getId(),
            null,
            uber.getId(),
            RevenueType.CONSOLIDATED,
            OPERATION_DATE,
            OPERATION_DATE,
            null,
            null,
            new BigDecimal("250.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            DataSource.MANUAL,
            "ACC-REV"
        ));

        var category = expenseCategories.findAllByActiveTrueOrderByNameAsc().stream()
            .filter(item -> "OTHER".equals(item.getCode()))
            .findFirst()
            .orElseThrow();
        expenses.create(new ExpenseService.CreateExpenseCommand(
            vehicle.getId(),
            day.getId(),
            shift.getId(),
            category.getId(),
            OPERATION_DATE,
            OPERATION_DATE,
            OPERATION_DATE,
            new BigDecimal("50.00"),
            ExpenseClassification.MIXED,
            AllocationMethod.MILEAGE_RATIO,
            null,
            null,
            null,
            "Despesa mista do fluxo de aceite"
        ));

        shifts.closeShift(
            shift.getId(),
            LocalDateTime.of(2035, 7, 16, 10, 0),
            new BigDecimal("50080.0"),
            "Pampulha",
            Set.of("Centro", "Pampulha")
        );

        var fueling = fuelings.create(new FuelingService.CreateFuelingCommand(
            vehicle.getId(),
            LocalDateTime.of(2035, 7, 16, 10, 15),
            new BigDecimal("50080.0"),
            "Posto de aceite",
            FuelType.GASOLINE,
            new BigDecimal("20.000"),
            new BigDecimal("5.000"),
            new BigDecimal("100.00"),
            true,
            "Abastecimento do fluxo de aceite"
        ));
        assertThat(fueling.getTotalAmount()).isEqualByComparingTo("100.00");

        var debt = obligations.create(new FinancialObligationService.CreateCommand(
            vehicle.getId(),
            ObligationType.FAMILY_LOAN,
            ObligationMode.FLEXIBLE,
            "Família",
            new BigDecimal("30000.00"),
            BigDecimal.ZERO,
            OPERATION_DATE,
            null,
            null,
            null,
            new BigDecimal("500.00"),
            null
        ));
        obligations.pay(
            debt.getId(),
            OPERATION_DATE,
            new BigDecimal("500.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "ACC-PAY",
            null
        );
        days.closeDay(day.getId(), new BigDecimal("50080.0"));

        var closing = mileageClosings.confirm(
            new MonthlyOdometerClosingService.ConfirmCommand(
                vehicle.getId(),
                YearMonth.from(OPERATION_DATE),
                false,
                null,
                null,
                null,
                null,
                true
            )
        );
        assertThat(closing.isManualAdjustment()).isFalse();
        assertThat(closing.getProfessionalKilometers()).isEqualByComparingTo("80.0");
        assertThat(vehicles.get(vehicle.getId()).getCurrentOdometer())
            .isEqualByComparingTo("50080.0");

        fuelings.create(new FuelingService.CreateFuelingCommand(
            vehicle.getId(),
            LocalDateTime.of(2035, 7, 15, 18, 0),
            new BigDecimal("49900.0"),
            "Posto histórico",
            FuelType.GASOLINE,
            new BigDecimal("10.000"),
            new BigDecimal("5.000"),
            new BigDecimal("50.00"),
            false,
            "Leitura histórica menor"
        ));
        assertThat(vehicles.get(vehicle.getId()).getCurrentOdometer())
            .isEqualByComparingTo("50080.0");

        var snapshot = dashboard.snapshot(OPERATION_DATE);
        assertThat(snapshot.dailyRevenue()).isEqualByComparingTo("250.00");
        assertThat(snapshot.dailyOperatingMargin()).isEqualByComparingTo("210.00");
        assertThat(snapshot.monthlyPersonalCash()).isEqualByComparingTo("-290.00");
        assertThat(snapshot.outstandingDebt()).isEqualByComparingTo("29500.00");

        mvc.perform(get("/expenses/new").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Profissional")))
            .andExpect(content().string(containsString("Proporcional à quilometragem")));

        mvc.perform(get("/").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Operação de hoje")));
    }
}
