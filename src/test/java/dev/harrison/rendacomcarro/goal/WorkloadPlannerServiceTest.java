package dev.harrison.rendacomcarro.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.harrison.rendacomcarro.goal.application.WorkloadCalculation;
import dev.harrison.rendacomcarro.goal.application.WorkloadPlannerService;
import dev.harrison.rendacomcarro.goal.domain.WorkloadPeriodicity;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorkloadPlannerServiceTest {
    private final WorkloadPlannerService service = new WorkloadPlannerService();

    @Test
    void dailyMultipliesTheDurationByEverySelectedDate() {
        YearMonth month = YearMonth.of(2026, 7);
        Set<LocalDate> dates = weekdays(month, 20);

        WorkloadCalculation result = service.calculate(
            month,
            WorkloadPeriodicity.DAILY,
            8 * 60 + 30,
            dates
        );

        assertThat(result.totalMinutes()).isEqualTo(10_200);
        assertThat(result.days()).hasSize(20)
            .allSatisfy(day -> assertThat(day.allocatedMinutes()).isEqualTo(510));
        assertThat(result.weeks().stream().mapToLong(WorkloadCalculation.WeekAllocation::allocatedMinutes).sum())
            .isEqualTo(result.totalMinutes());
    }

    @Test
    void monthlyDistributesRemainderMinutesInChronologicalOrder() {
        YearMonth month = YearMonth.of(2026, 7);
        Set<LocalDate> dates = Set.of(
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 2),
            LocalDate.of(2026, 7, 3)
        );

        WorkloadCalculation result = service.calculate(
            month,
            WorkloadPeriodicity.MONTHLY,
            10,
            dates
        );

        assertThat(result.totalMinutes()).isEqualTo(10);
        assertThat(result.days())
            .extracting(WorkloadCalculation.DayAllocation::allocatedMinutes)
            .containsExactly(4L, 3L, 3L);
    }

    @Test
    void weeklyAssignsAFullLoadToEveryNonEmptyInternalWeek() {
        YearMonth month = YearMonth.of(2026, 7);
        Set<LocalDate> dates = new LinkedHashSet<>();
        dates.addAll(dates(2026, 7, 6, 7, 8, 9));
        dates.addAll(dates(2026, 7, 13, 14, 15, 16, 17));
        dates.addAll(dates(2026, 7, 20, 21, 22, 23, 24, 25));

        WorkloadCalculation result = service.calculate(
            month,
            WorkloadPeriodicity.WEEKLY,
            40 * 60,
            dates
        );

        assertThat(result.totalMinutes()).isEqualTo(7_200);
        assertThat(result.weeks())
            .extracting(WorkloadCalculation.WeekAllocation::selectedDays)
            .containsExactly(4, 5, 6);
        assertThat(result.weeks())
            .extracting(WorkloadCalculation.WeekAllocation::allocatedMinutes)
            .containsOnly(2_400L);
    }

    @Test
    void weeklyProratesBoundaryUsingTheMostFrequentExactWeekdayPattern() {
        YearMonth month = YearMonth.of(2026, 7);
        Set<LocalDate> dates = new LinkedHashSet<>();
        dates.addAll(dates(2026, 7, 1, 2));
        dates.addAll(dates(2026, 7, 6, 7, 8, 9, 10));
        dates.addAll(dates(2026, 7, 13, 14, 15, 16, 17));

        WorkloadCalculation result = service.calculate(
            month,
            WorkloadPeriodicity.WEEKLY,
            40 * 60,
            dates
        );

        WorkloadCalculation.WeekAllocation boundary = result.weeks().getFirst();
        assertThat(boundary.weekStart()).isEqualTo(LocalDate.of(2026, 6, 29));
        assertThat(boundary.allocatedMinutes()).isEqualTo(960);
        assertThat(boundary.inferredPattern()).containsExactlyInAnyOrder(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        );
    }

    @Test
    void weeklyTieUsesTheNearestInternalPatternForEachBoundary() {
        YearMonth month = YearMonth.of(2026, 7);
        Set<LocalDate> dates = new LinkedHashSet<>();
        dates.addAll(dates(2026, 7, 1, 2));
        dates.addAll(dates(2026, 7, 6, 7, 8, 9, 10));
        dates.addAll(dates(2026, 7, 14, 15, 16, 17, 18));
        dates.addAll(dates(2026, 7, 27, 28));

        WorkloadCalculation result = service.calculate(
            month,
            WorkloadPeriodicity.WEEKLY,
            40 * 60,
            dates
        );

        WorkloadCalculation.WeekAllocation firstBoundary = result.weeks().getFirst();
        WorkloadCalculation.WeekAllocation lastBoundary = result.weeks().getLast();
        assertThat(firstBoundary.inferredPattern()).contains(DayOfWeek.MONDAY);
        assertThat(firstBoundary.inferredPattern()).doesNotContain(DayOfWeek.SATURDAY);
        assertThat(lastBoundary.inferredPattern()).contains(DayOfWeek.SATURDAY);
        assertThat(lastBoundary.inferredPattern()).doesNotContain(DayOfWeek.MONDAY);
    }

    @Test
    void weeklyBoundaryWithoutInternalEvidenceUsesFiveDays() {
        YearMonth month = YearMonth.of(2026, 7);

        WorkloadCalculation result = service.calculate(
            month,
            WorkloadPeriodicity.WEEKLY,
            40 * 60,
            dates(2026, 7, 1, 2)
        );

        assertThat(result.totalMinutes()).isEqualTo(960);
        assertThat(result.weeks()).singleElement().satisfies(week -> {
            assertThat(week.selectedDays()).isEqualTo(2);
            assertThat(week.allocatedMinutes()).isEqualTo(960);
            assertThat(week.inferredPattern()).isEmpty();
        });
    }

    @Test
    void weeklyBoundaryNeverExceedsOneFullWeeklyLoad() {
        YearMonth month = YearMonth.of(2026, 7);

        WorkloadCalculation result = service.calculate(
            month,
            WorkloadPeriodicity.WEEKLY,
            40 * 60,
            dates(2026, 7, 27, 28, 29, 30, 31)
        );

        assertThat(result.totalMinutes()).isEqualTo(2_400);
    }

    @Test
    void weeklyRoundsHalfAMinuteUpOnceBeforeDailyDistribution() {
        YearMonth month = YearMonth.of(2026, 7);
        Set<LocalDate> dates = new LinkedHashSet<>();
        dates.addAll(dates(2026, 7, 1, 2));
        dates.addAll(dates(2026, 7, 6, 7, 8, 9));

        WorkloadCalculation result = service.calculate(
            month,
            WorkloadPeriodicity.WEEKLY,
            1,
            dates
        );

        assertThat(result.weeks().getFirst().allocatedMinutes()).isEqualTo(1);
        assertThat(result.totalMinutes()).isEqualTo(2);
        assertThat(result.days().stream().mapToLong(WorkloadCalculation.DayAllocation::allocatedMinutes).sum())
            .isEqualTo(2);
    }

    @Test
    void rejectsInvalidDurationAndDates() {
        YearMonth month = YearMonth.of(2026, 7);

        assertThatThrownBy(() -> service.calculate(
            month,
            WorkloadPeriodicity.DAILY,
            0,
            dates(2026, 7, 1)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maior que zero");

        assertThatThrownBy(() -> service.calculate(
            month,
            WorkloadPeriodicity.DAILY,
            60,
            Set.of()
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dia planejado");

        assertThatThrownBy(() -> service.calculate(
            month,
            WorkloadPeriodicity.DAILY,
            60,
            dates(2026, 7, 5)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Domingos");

        assertThatThrownBy(() -> service.calculate(
            month,
            WorkloadPeriodicity.DAILY,
            60,
            dates(2026, 8, 1)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mês");
    }

    private static Set<LocalDate> dates(int year, int month, int... days) {
        LinkedHashSet<LocalDate> values = new LinkedHashSet<>();
        for (int day : days) values.add(LocalDate.of(year, month, day));
        return values;
    }

    private static Set<LocalDate> weekdays(YearMonth month, int count) {
        LinkedHashSet<LocalDate> values = new LinkedHashSet<>();
        LocalDate current = month.atDay(1);
        while (values.size() < count && YearMonth.from(current).equals(month)) {
            if (current.getDayOfWeek() != DayOfWeek.SUNDAY) values.add(current);
            current = current.plusDays(1);
        }
        return values;
    }
}
