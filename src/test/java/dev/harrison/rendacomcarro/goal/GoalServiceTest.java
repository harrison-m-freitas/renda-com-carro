package dev.harrison.rendacomcarro.goal;

import dev.harrison.rendacomcarro.goal.application.GoalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GoalServiceTest {
    private final GoalService service = new GoalService(null, null);
    @Test
    void excludesSundaysFromRemainingDailyGoal(){
        var projection=service.project(YearMonth.of(2026,7),new BigDecimal("2100.00"),new BigDecimal("700.00"),Set.of(
            LocalDate.of(2026,7,20), LocalDate.of(2026,7,21), LocalDate.of(2026,7,26)));
        assertThat(projection.eligibleRemainingDays()).isEqualTo(2);
        assertThat(projection.requiredPerDay()).isEqualByComparingTo("700.00");
    }
}
