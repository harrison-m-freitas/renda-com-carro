package dev.harrison.rendacomcarro.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.application.DraftPayloadValidator;
import dev.harrison.rendacomcarro.draft.application.FormDraftDefinitionRegistry;
import dev.harrison.rendacomcarro.draft.application.definition.ExpenseDraftDefinition;
import dev.harrison.rendacomcarro.draft.application.definition.MileageClosingDraftDefinition;
import dev.harrison.rendacomcarro.draft.application.definition.MonthlyGoalDraftDefinition;
import dev.harrison.rendacomcarro.draft.application.definition.ObligationDraftDefinition;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FormDraftDefinitionTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private ExpenseDraftDefinition expense;
    private MileageClosingDraftDefinition mileage;
    private MonthlyGoalDraftDefinition goal;
    private ObligationDraftDefinition obligation;

    @BeforeEach
    void setUp() {
        DraftPayloadValidator validator = new DraftPayloadValidator();
        expense = new ExpenseDraftDefinition(validator);
        mileage = new MileageClosingDraftDefinition(validator);
        goal = new MonthlyGoalDraftDefinition(validator);
        obligation = new ObligationDraftDefinition(validator);
    }

    @Test
    void expenseRejectsUnknownFieldAndNormalizesCurrentKey() {
        ObjectNode payload = mapper.createObjectNode()
            .put("vehicleId", UUID.randomUUID().toString())
            .put("categoryId", UUID.randomUUID().toString())
            .put("expenseDate", "2026-07-13")
            .put("competenceMonth", "2026-07")
            .put("amount", "120,50")
            .put("classification", "PROFESSIONAL")
            .put("paymentStatus", "PENDING");

        assertThat(expense.schemaVersion()).isEqualTo(2);
        assertThat(expense.normalizeContextKey(" current ")).isEqualTo("current");
        assertThatNoException().isThrownBy(() -> expense.normalizeAndValidate(payload, 2));

        payload.put("calculatedProfessionalAmount", "120.50");
        assertThatThrownBy(() -> expense.normalizeAndValidate(payload, 2))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Campo de rascunho não permitido");
    }

    @Test
    void expenseMigratesLegacyPaymentStateAndAcceptsOperationalContext() {
        UUID dayId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        ObjectNode legacy = mapper.createObjectNode()
            .put("vehicleId", UUID.randomUUID().toString())
            .put("categoryId", UUID.randomUUID().toString())
            .put("operationalDayId", dayId.toString())
            .put("shiftId", shiftId.toString())
            .put("expenseDate", "2026-07-13")
            .put("paidDate", "2026-07-13")
            .put("amount", "120,50")
            .put("classification", "PROFESSIONAL");

        ObjectNode migrated = expense.migrate(1, legacy);
        ObjectNode normalized = expense.normalizeAndValidate(migrated, 2, false);

        assertThat(normalized.path("paymentStatus").asText()).isEqualTo("PAID");
        assertThat(normalized.path("operationalDayId").asText()).isEqualTo(dayId.toString());
        assertThat(normalized.path("shiftId").asText()).isEqualTo(shiftId.toString());
    }

    @Test
    void pendingExpenseDraftRemovesPaidDateAndManualExtremesAreRejected() {
        ObjectNode pending = mapper.createObjectNode()
            .put("paymentStatus", "PENDING")
            .put("paidDate", "2026-07-13")
            .put("classification", "PROFESSIONAL");

        ObjectNode normalized = expense.normalizeAndValidate(pending, 1, false);
        assertThat(normalized.has("paidDate")).isFalse();

        ObjectNode percentage = mapper.createObjectNode()
            .put("classification", "MIXED")
            .put("allocationMethod", "MANUAL_PERCENTAGE")
            .put("professionalPercentagePercent", "100")
            .put("adjustmentReason", "Rateio manual");
        assertThatThrownBy(() -> expense.normalizeAndValidate(percentage, 2, false))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Profissional");

        ObjectNode fixed = mapper.createObjectNode()
            .put("amount", "100,00")
            .put("classification", "MIXED")
            .put("allocationMethod", "FIXED_AMOUNT")
            .put("professionalFixedAmount", "100,00")
            .put("adjustmentReason", "Rateio manual");
        assertThatThrownBy(() -> expense.normalizeAndValidate(fixed, 2, false))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Profissional");
    }

    @Test
    void manualMileageCorrectionRequiresReason() {
        ObjectNode payload = mapper.createObjectNode()
            .put("manualAdjustment", true)
            .put("initialOdometer", "10000,0")
            .put("finalOdometer", "10100,0")
            .put("professionalKilometers", "80,0");

        assertThatThrownBy(() -> mileage.normalizeAndValidate(payload, 3))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("justificativa");
    }

    @Test
    void automaticMileageDraftRemovesManualValues() {
        ObjectNode payload = mapper.createObjectNode()
            .put("manualAdjustment", false)
            .put("initialOdometer", "10000,0")
            .put("finalOdometer", "10100,0")
            .put("professionalKilometers", "80,0")
            .put("adjustmentReason", "Não deve permanecer")
            .put("confirmWarnings", true);

        ObjectNode normalized = mileage.normalizeAndValidate(payload, 3);

        assertThat(normalized.has("initialOdometer")).isFalse();
        assertThat(normalized.has("finalOdometer")).isFalse();
        assertThat(normalized.has("professionalKilometers")).isFalse();
        assertThat(normalized.has("adjustmentReason")).isFalse();
    }

    @Test
    void goalDraftSchemaThreePreservesWeeklySourceWithoutRequiringDatesDuringAutosave() {
        ObjectNode payload = mapper.createObjectNode()
            .put("month", "2026-07")
            .put("personalNetGoal", "2000,00")
            .put("operationalGoal", "3000,00")
            .put("workloadPeriodicity", "weekly")
            .put("workloadHours", "40")
            .put("workloadMinutes", "0");
        payload.putArray("vehicleIds").add(UUID.randomUUID().toString());

        ObjectNode normalized = goal.normalizeAndValidate(payload, 2, false);

        assertThat(goal.schemaVersion()).isEqualTo(3);
        assertThat(normalized.path("workloadPeriodicity").asText()).isEqualTo("WEEKLY");
        assertThat(normalized.path("workloadHours").asLong()).isEqualTo(40);
        assertThat(normalized.path("workloadMinutes").asInt()).isZero();
        assertThat(normalized.has("plannedDates")).isFalse();

        assertThatThrownBy(() -> goal.normalizeAndValidate(payload.deepCopy(), 2, true))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Dias planejados");
    }

    @Test
    void monthlyGoalDraftNormalizesVehicleIdArray() {
        UUID idA = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID idB = UUID.fromString("00000000-0000-0000-0000-000000000002");
        ObjectNode payload = mapper.createObjectNode()
            .put("month", "2026-07")
            .put("personalNetGoal", "2000,00")
            .put("operationalGoal", "3000,00")
            .put("workloadPeriodicity", "MONTHLY")
            .put("workloadHours", "160")
            .put("workloadMinutes", "0")
            .put("plannedDates", "2026-07-01");
        payload.putArray("vehicleIds")
            .add(idB.toString())
            .add(idA.toString())
            .add(idA.toString());

        ObjectNode normalized = goal.normalizeAndValidate(payload, 3, true);

        assertThat(goal.schemaVersion()).isEqualTo(3);
        assertThat(normalized.withArray("vehicleIds"))
            .extracting(node -> node.asText())
            .containsExactly(idA.toString(), idB.toString());
    }

    @Test
    void monthlyGoalDraftRequiresVehicleIdsAtFirstValidatedStep() {
        ObjectNode payload = mapper.createObjectNode()
            .put("month", "2026-07")
            .put("personalNetGoal", "2000,00")
            .put("operationalGoal", "3000,00");

        assertThatThrownBy(() -> goal.normalizeAndValidate(payload, 1, true))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Veículos");
    }

    @Test
    void goalRejectsInvalidMinutesSundayAndDatesOutsideMonth() {
        ObjectNode payload = mapper.createObjectNode()
            .put("month", "2026-07")
            .put("personalNetGoal", "2000,00")
            .put("operationalGoal", "3000,00")
            .put("workloadPeriodicity", "WEEKLY")
            .put("workloadHours", "40")
            .put("workloadMinutes", "60")
            .put("plannedDates", "2026-07-05");
        payload.putArray("vehicleIds").add(UUID.randomUUID().toString());

        assertThatThrownBy(() -> goal.normalizeAndValidate(payload, 3))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("minutos");

        payload.put("workloadMinutes", "0");
        assertThatThrownBy(() -> goal.normalizeAndValidate(payload, 3))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Domingos");

        payload.put("plannedDates", "2026-08-01");
        assertThatThrownBy(() -> goal.normalizeAndValidate(payload, 3))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("mês da meta");
    }

    @Test
    void goalRejectsLegacyCalculatedHoursField() {
        ObjectNode payload = mapper.createObjectNode()
            .put("month", "2026-07")
            .put("workloadPeriodicity", "MONTHLY")
            .put("workloadHours", "160")
            .put("workloadMinutes", "0")
            .put("plannedHours", "160");

        assertThatThrownBy(() -> goal.normalizeAndValidate(payload, 1, false))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Campo de rascunho não permitido");
    }

    @Test
    void obligationStructuredModeRequiresScheduleFieldsAtStepThree() {
        ObjectNode payload = mapper.createObjectNode()
            .put("creditor", "Banco")
            .put("type", "BANK_FINANCING")
            .put("mode", "STRUCTURED")
            .put("principal", "30000,00")
            .put("startDate", "2026-07-13");

        assertThatThrownBy(() -> obligation.normalizeAndValidate(payload, 3))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("primeiro vencimento");
    }

    @Test
    void registryFindsEverySupportedDefinition() {
        FormDraftDefinitionRegistry registry = new FormDraftDefinitionRegistry(
            List.of(expense, mileage, goal, obligation)
        );

        assertThat(registry.require(FormDraftType.EXPENSE)).isSameAs(expense);
        assertThat(registry.require(FormDraftType.MILEAGE_CLOSING)).isSameAs(mileage);
        assertThat(registry.require(FormDraftType.MONTHLY_GOAL)).isSameAs(goal);
        assertThat(registry.require(FormDraftType.OBLIGATION)).isSameAs(obligation);
    }
}
