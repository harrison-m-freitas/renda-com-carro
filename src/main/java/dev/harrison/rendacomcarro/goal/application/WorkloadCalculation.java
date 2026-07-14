package dev.harrison.rendacomcarro.goal.application;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record WorkloadCalculation(
    long totalMinutes,
    List<WeekAllocation> weeks,
    List<DayAllocation> days
) {
    public WorkloadCalculation {
        if (totalMinutes < 0) {
            throw new IllegalArgumentException("O total calculado não pode ser negativo.");
        }
        weeks = List.copyOf(weeks);
        days = List.copyOf(days);
    }

    public record WeekAllocation(
        LocalDate weekStart,
        LocalDate weekEnd,
        int selectedDays,
        long allocatedMinutes,
        Set<DayOfWeek> inferredPattern
    ) {
        public WeekAllocation {
            inferredPattern = Set.copyOf(inferredPattern);
        }
    }

    public record DayAllocation(LocalDate date, long allocatedMinutes) {}
}
