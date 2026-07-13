package dev.harrison.rendacomcarro.shared;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.harrison.rendacomcarro.attachment.domain.OwnerType;
import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import dev.harrison.rendacomcarro.finance.domain.InstallmentStatus;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.domain.ObligationStatus;
import dev.harrison.rendacomcarro.finance.domain.ObligationType;
import dev.harrison.rendacomcarro.goal.domain.GoalStatus;
import dev.harrison.rendacomcarro.operation.domain.DataSource;
import dev.harrison.rendacomcarro.operation.domain.OperationalDayStatus;
import dev.harrison.rendacomcarro.operation.domain.RevenueType;
import dev.harrison.rendacomcarro.operation.domain.ShiftStatus;
import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import dev.harrison.rendacomcarro.vehicle.domain.VehicleStatus;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VisibleEnumLabelTest {
    private static final List<Class<? extends Enum<?>>> VISIBLE_ENUMS = List.of(
        FuelType.class,
        VehicleStatus.class,
        OperationalDayStatus.class,
        ShiftStatus.class,
        DataSource.class,
        RevenueType.class,
        ExpenseClassification.class,
        AllocationMethod.class,
        GoalStatus.class,
        ObligationType.class,
        ObligationMode.class,
        ObligationStatus.class,
        InstallmentStatus.class,
        OwnerType.class
    );

    private static final Set<String> APPROVED_ACRONYM_LABELS = Set.of("API");

    @Test
    void everyVisibleEnumHasFriendlyPortugueseLabel() {
        for (Class<? extends Enum<?>> type : VISIBLE_ENUMS) {
            assertTrue(LabeledEnum.class.isAssignableFrom(type), type.getName());
            for (Enum<?> constant : type.getEnumConstants()) {
                String label = ((LabeledEnum) constant).getLabel();
                assertNotNull(label, constant.name());
                assertFalse(label.isBlank(), constant.name());

                if (!APPROVED_ACRONYM_LABELS.contains(label)) {
                    assertNotEquals(constant.name(), label, constant.name());
                    assertFalse(label.matches("[A-Z0-9_]+"), constant.name());
                }
            }
        }
    }
}
