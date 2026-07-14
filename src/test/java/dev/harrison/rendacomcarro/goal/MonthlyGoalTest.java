package dev.harrison.rendacomcarro.goal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.harrison.rendacomcarro.goal.domain.MonthlyGoal;
import dev.harrison.rendacomcarro.goal.domain.WorkloadPeriodicity;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class MonthlyGoalTest {
    @Test
    void exposesEnteredAndCalculatedHoursAndMinutesForPresentation() {
        MonthlyGoal goal = MonthlyGoal.create(
            YearMonth.of(2027, 4),
            new BigDecimal("2500.00"),
            new BigDecimal("4000.00"),
            WorkloadPeriodicity.WEEKLY,
            2_430,
            10_230
        );

        assertThat(goal.getEnteredHours()).isEqualTo(40);
        assertThat(goal.getEnteredRemainderMinutes()).isEqualTo(30);
        assertThat(goal.getCalculatedHours()).isEqualTo(170);
        assertThat(goal.getCalculatedRemainderMinutes()).isEqualTo(30);
    }
}
