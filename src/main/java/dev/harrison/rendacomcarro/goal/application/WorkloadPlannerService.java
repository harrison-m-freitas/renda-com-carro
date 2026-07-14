package dev.harrison.rendacomcarro.goal.application;

import dev.harrison.rendacomcarro.goal.domain.WorkloadPeriodicity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Service;

@Service
public class WorkloadPlannerService {
    private static final int FALLBACK_WEEK_DAYS = 5;

    public WorkloadCalculation calculate(
        YearMonth month,
        WorkloadPeriodicity periodicity,
        long enteredDurationMinutes,
        Set<LocalDate> plannedDates
    ) {
        TreeSet<LocalDate> dates = validateAndNormalize(
            month,
            periodicity,
            enteredDurationMinutes,
            plannedDates
        );

        return switch (periodicity) {
            case DAILY -> calculateDaily(enteredDurationMinutes, dates);
            case WEEKLY -> calculateWeekly(month, enteredDurationMinutes, dates);
            case MONTHLY -> calculateMonthly(enteredDurationMinutes, dates);
        };
    }

    private WorkloadCalculation calculateDaily(
        long enteredDurationMinutes,
        TreeSet<LocalDate> dates
    ) {
        Map<LocalDate, Long> allocatedByDate = new LinkedHashMap<>();
        dates.forEach(date -> allocatedByDate.put(date, enteredDurationMinutes));
        return buildCalculation(allocatedByDate, Map.of());
    }

    private WorkloadCalculation calculateMonthly(
        long enteredDurationMinutes,
        TreeSet<LocalDate> dates
    ) {
        Map<LocalDate, Long> allocatedByDate = distribute(enteredDurationMinutes, List.copyOf(dates));
        return buildCalculation(allocatedByDate, Map.of());
    }

    private WorkloadCalculation calculateWeekly(
        YearMonth month,
        long enteredDurationMinutes,
        TreeSet<LocalDate> dates
    ) {
        TreeMap<LocalDate, List<LocalDate>> datesByWeek = groupByWeek(dates);
        List<PatternObservation> internalPatterns = datesByWeek.entrySet().stream()
            .filter(entry -> !isBoundaryWeek(month, entry.getKey()))
            .map(entry -> new PatternObservation(
                entry.getKey(),
                weekdaySet(entry.getValue())
            ))
            .toList();

        Map<LocalDate, Long> allocatedByDate = new LinkedHashMap<>();
        Map<LocalDate, Set<DayOfWeek>> inferredPatterns = new LinkedHashMap<>();

        for (Map.Entry<LocalDate, List<LocalDate>> entry : datesByWeek.entrySet()) {
            LocalDate weekStart = entry.getKey();
            List<LocalDate> selectedDates = entry.getValue();
            long weekMinutes = enteredDurationMinutes;

            if (isBoundaryWeek(month, weekStart)) {
                Set<DayOfWeek> inferredPattern = inferPattern(weekStart, internalPatterns);
                int expectedDays = inferredPattern.isEmpty()
                    ? FALLBACK_WEEK_DAYS
                    : inferredPattern.size();
                int selectedDays = Math.min(selectedDates.size(), expectedDays);
                weekMinutes = multiplyAndDivideHalfUp(
                    enteredDurationMinutes,
                    selectedDays,
                    expectedDays
                );
                inferredPatterns.put(weekStart, inferredPattern);
            }

            allocatedByDate.putAll(distribute(weekMinutes, selectedDates));
        }

        return buildCalculation(allocatedByDate, inferredPatterns);
    }

    private TreeSet<LocalDate> validateAndNormalize(
        YearMonth month,
        WorkloadPeriodicity periodicity,
        long enteredDurationMinutes,
        Set<LocalDate> plannedDates
    ) {
        if (month == null) {
            throw new IllegalArgumentException("O mês da meta é obrigatório.");
        }
        if (periodicity == null) {
            throw new IllegalArgumentException("A periodicidade da jornada é obrigatória.");
        }
        if (enteredDurationMinutes <= 0) {
            throw new IllegalArgumentException("A duração informada deve ser maior que zero.");
        }
        if (plannedDates == null || plannedDates.isEmpty()) {
            throw new IllegalArgumentException("Informe pelo menos um dia planejado.");
        }

        TreeSet<LocalDate> normalized = new TreeSet<>();
        for (LocalDate date : plannedDates) {
            if (date == null) {
                throw new IllegalArgumentException("Os dias planejados devem ser válidos.");
            }
            if (!YearMonth.from(date).equals(month)) {
                throw new IllegalArgumentException(
                    "Todos os dias planejados devem pertencer ao mês da meta."
                );
            }
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                throw new IllegalArgumentException(
                    "Domingos não podem ser adicionados aos dias planejados."
                );
            }
            normalized.add(date);
        }
        return normalized;
    }

    private TreeMap<LocalDate, List<LocalDate>> groupByWeek(Set<LocalDate> dates) {
        TreeMap<LocalDate, List<LocalDate>> grouped = new TreeMap<>();
        for (LocalDate date : dates) {
            grouped.computeIfAbsent(weekStart(date), ignored -> new ArrayList<>()).add(date);
        }
        grouped.values().forEach(values -> values.sort(Comparator.naturalOrder()));
        return grouped;
    }

    private LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private boolean isBoundaryWeek(YearMonth month, LocalDate weekStart) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        LocalDate weekEnd = weekStart.plusDays(6);
        return weekStart.isBefore(monthStart) || weekEnd.isAfter(monthEnd);
    }

    private Set<DayOfWeek> weekdaySet(List<LocalDate> dates) {
        EnumSet<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);
        dates.forEach(date -> weekdays.add(date.getDayOfWeek()));
        return Set.copyOf(weekdays);
    }

    private Set<DayOfWeek> inferPattern(
        LocalDate boundaryWeekStart,
        List<PatternObservation> observations
    ) {
        if (observations.isEmpty()) return Set.of();

        Map<Set<DayOfWeek>, List<PatternObservation>> observationsByPattern = new LinkedHashMap<>();
        observations.forEach(observation -> observationsByPattern
            .computeIfAbsent(observation.weekdays(), ignored -> new ArrayList<>())
            .add(observation));

        int highestFrequency = observationsByPattern.values().stream()
            .mapToInt(List::size)
            .max()
            .orElse(0);

        return observationsByPattern.entrySet().stream()
            .filter(entry -> entry.getValue().size() == highestFrequency)
            .map(entry -> closestCandidate(boundaryWeekStart, entry.getKey(), entry.getValue()))
            .min(Comparator
                .comparingLong(PatternCandidate::distanceInWeeks)
                .thenComparing(PatternCandidate::representativeWeekStart))
            .map(PatternCandidate::weekdays)
            .orElse(Set.of());
    }

    private PatternCandidate closestCandidate(
        LocalDate boundaryWeekStart,
        Set<DayOfWeek> weekdays,
        List<PatternObservation> observations
    ) {
        PatternObservation closest = observations.stream()
            .min(Comparator
                .comparingLong((PatternObservation observation) -> distanceInWeeks(
                    boundaryWeekStart,
                    observation.weekStart()
                ))
                .thenComparing(PatternObservation::weekStart))
            .orElseThrow();

        return new PatternCandidate(
            weekdays,
            closest.weekStart(),
            distanceInWeeks(boundaryWeekStart, closest.weekStart())
        );
    }

    private long distanceInWeeks(LocalDate firstWeekStart, LocalDate secondWeekStart) {
        return Math.abs(ChronoUnit.WEEKS.between(firstWeekStart, secondWeekStart));
    }

    private long multiplyAndDivideHalfUp(long value, int numerator, int denominator) {
        return BigDecimal.valueOf(value)
            .multiply(BigDecimal.valueOf(numerator))
            .divide(BigDecimal.valueOf(denominator), 0, RoundingMode.HALF_UP)
            .longValueExact();
    }

    private Map<LocalDate, Long> distribute(long totalMinutes, List<LocalDate> selectedDates) {
        if (selectedDates.isEmpty()) return Map.of();

        List<LocalDate> sortedDates = selectedDates.stream().sorted().toList();
        long baseMinutes = totalMinutes / sortedDates.size();
        long remainder = totalMinutes % sortedDates.size();
        Map<LocalDate, Long> allocation = new LinkedHashMap<>();

        for (int index = 0; index < sortedDates.size(); index++) {
            allocation.put(
                sortedDates.get(index),
                baseMinutes + (index < remainder ? 1 : 0)
            );
        }
        return allocation;
    }

    private WorkloadCalculation buildCalculation(
        Map<LocalDate, Long> allocatedByDate,
        Map<LocalDate, Set<DayOfWeek>> inferredPatterns
    ) {
        List<WorkloadCalculation.DayAllocation> dayAllocations = allocatedByDate.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> new WorkloadCalculation.DayAllocation(entry.getKey(), entry.getValue()))
            .toList();

        TreeMap<LocalDate, List<WorkloadCalculation.DayAllocation>> daysByWeek = new TreeMap<>();
        dayAllocations.forEach(day -> daysByWeek
            .computeIfAbsent(weekStart(day.date()), ignored -> new ArrayList<>())
            .add(day));

        List<WorkloadCalculation.WeekAllocation> weekAllocations = daysByWeek.entrySet()
            .stream()
            .map(entry -> new WorkloadCalculation.WeekAllocation(
                entry.getKey(),
                entry.getKey().plusDays(6),
                entry.getValue().size(),
                entry.getValue().stream()
                    .mapToLong(WorkloadCalculation.DayAllocation::allocatedMinutes)
                    .sum(),
                inferredPatterns.getOrDefault(entry.getKey(), Set.of())
            ))
            .toList();

        long totalMinutes = dayAllocations.stream()
            .mapToLong(WorkloadCalculation.DayAllocation::allocatedMinutes)
            .sum();
        return new WorkloadCalculation(totalMinutes, weekAllocations, dayAllocations);
    }

    private record PatternObservation(
        LocalDate weekStart,
        Set<DayOfWeek> weekdays
    ) {}

    private record PatternCandidate(
        Set<DayOfWeek> weekdays,
        LocalDate representativeWeekStart,
        long distanceInWeeks
    ) {}
}
