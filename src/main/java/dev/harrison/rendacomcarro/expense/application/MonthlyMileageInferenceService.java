package dev.harrison.rendacomcarro.expense.application;

import dev.harrison.rendacomcarro.expense.infrastructure.MonthlyOdometerClosingRepository;
import dev.harrison.rendacomcarro.fuel.domain.Fueling;
import dev.harrison.rendacomcarro.fuel.infrastructure.FuelingRepository;
import dev.harrison.rendacomcarro.operation.domain.OperationalDay;
import dev.harrison.rendacomcarro.operation.domain.OperationalDayStatus;
import dev.harrison.rendacomcarro.operation.domain.Shift;
import dev.harrison.rendacomcarro.operation.domain.ShiftStatus;
import dev.harrison.rendacomcarro.operation.infrastructure.OperationalDayRepository;
import dev.harrison.rendacomcarro.operation.infrastructure.ShiftRepository;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.OdometerReadingSource;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonthlyMileageInferenceService {
    private static final BigDecimal DISTANCE_GAP_TOLERANCE = new BigDecimal("1.0");

    private final VehicleService vehicles;
    private final OperationalDayRepository days;
    private final ShiftRepository shifts;
    private final FuelingRepository fuelings;
    private final MonthlyOdometerClosingRepository closings;

    public MonthlyMileageInferenceService(
        VehicleService vehicles,
        OperationalDayRepository days,
        ShiftRepository shifts,
        FuelingRepository fuelings,
        MonthlyOdometerClosingRepository closings
    ) {
        this.vehicles = vehicles;
        this.days = days;
        this.shifts = shifts;
        this.fuelings = fuelings;
        this.closings = closings;
    }

    @Transactional(readOnly = true)
    public MonthlyMileagePreview infer(UUID vehicleId, YearMonth month) {
        if (vehicleId == null || month == null) {
            throw new IllegalArgumentException("Veículo e mês são obrigatórios");
        }

        Vehicle vehicle = vehicles.get(vehicleId);
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime exclusiveEnd = month.plusMonths(1).atDay(1).atStartOfDay();

        List<OperationalDay> monthlyDays = days
            .findAllByVehicleIdAndDateBetweenOrderByDateAsc(vehicleId, startDate, endDate);
        List<Shift> monthlyShifts = shifts.findAllForVehicleBetween(vehicleId, startTime, exclusiveEnd);
        List<Fueling> monthlyFuelings = fuelings
            .findAllByVehicleIdAndFueledAtGreaterThanEqualAndFueledAtLessThanOrderByFueledAtAsc(
                vehicleId, startTime, exclusiveEnd);

        List<MileageAlert> alerts = new ArrayList<>();
        if (closings.findByVehicleIdAndReferenceMonth(vehicleId, startDate).isPresent()) {
            alerts.add(blocking("DUPLICATE_CLOSING", "Já existe fechamento para este veículo e mês."));
        }
        if (monthlyDays.stream().anyMatch(day -> day.getStatus() == OperationalDayStatus.IN_PROGRESS)) {
            alerts.add(blocking("OPEN_OPERATIONAL_DAY", "Existe dia operacional em andamento no mês."));
        }
        if (monthlyShifts.stream().anyMatch(shift -> shift.getStatus() == ShiftStatus.OPEN)) {
            alerts.add(blocking("OPEN_SHIFT", "Existe turno em andamento no mês."));
        }

        Reading initial = inferInitial(
            vehicleId, startDate, vehicle, monthlyDays, monthlyShifts, monthlyFuelings);
        Reading end = inferFinal(vehicle, startTime, exclusiveEnd, monthlyDays, monthlyShifts, monthlyFuelings);

        if (initial == null) {
            alerts.add(blocking("MISSING_INITIAL_ODOMETER", "Não foi possível inferir o odômetro inicial."));
        }
        if (end == null) {
            alerts.add(blocking("MISSING_FINAL_ODOMETER", "Não foi possível inferir o odômetro final."));
        }

        addChronologicalRegressionAlert(initial, vehicle, monthlyDays, monthlyShifts, monthlyFuelings, alerts);

        BigDecimal professional = distance(monthlyShifts.stream()
            .filter(shift -> shift.getStatus() == ShiftStatus.CLOSED)
            .map(Shift::getDistance)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal total = BigDecimal.ZERO.setScale(1);
        BigDecimal personal = BigDecimal.ZERO.setScale(1);
        BigDecimal percentage = BigDecimal.ZERO.setScale(4);

        if (initial != null && end != null) {
            BigDecimal rawTotal = end.value().subtract(initial.value());
            if (rawTotal.signum() < 0) {
                addBlockingOnce(alerts, "ODOMETER_REGRESSION",
                    "O odômetro final inferido é menor que o odômetro inicial.");
            } else {
                total = distance(rawTotal);
                if (professional.compareTo(total) > 0) {
                    alerts.add(blocking(
                        "PROFESSIONAL_EXCEEDS_TOTAL",
                        "A soma dos quilômetros profissionais excede a distância total do mês."
                    ));
                } else {
                    personal = distance(total.subtract(professional));
                    percentage = total.signum() == 0
                        ? BigDecimal.ZERO.setScale(4)
                        : professional.divide(total, 4, RoundingMode.HALF_UP);
                }
            }
        }

        addDayShiftGapWarnings(monthlyDays, monthlyShifts, alerts);
        if (total.signum() > 0 && professional.signum() == 0) {
            alerts.add(warning(
                "NO_PROFESSIONAL_DISTANCE",
                "Há distância total no mês, mas nenhum turno fechado contribuiu com quilômetros profissionais."
            ));
        }

        int closedDays = (int) monthlyDays.stream()
            .filter(day -> day.getStatus() == OperationalDayStatus.CLOSED)
            .count();
        int closedShifts = (int) monthlyShifts.stream()
            .filter(shift -> shift.getStatus() == ShiftStatus.CLOSED)
            .count();

        return new MonthlyMileagePreview(
            vehicleId,
            month,
            initial == null ? null : initial.value(),
            end == null ? null : end.value(),
            total,
            professional,
            personal,
            percentage,
            initial == null ? null : initial.origin(),
            end == null ? null : end.origin(),
            closedDays,
            closedShifts,
            monthlyFuelings.size(),
            alerts,
            LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        );
    }

    private Reading inferInitial(
        UUID vehicleId,
        LocalDate referenceMonth,
        Vehicle vehicle,
        List<OperationalDay> monthlyDays,
        List<Shift> monthlyShifts,
        List<Fueling> monthlyFuelings
    ) {
        var previous = closings.findTopByVehicleIdAndReferenceMonthBeforeOrderByReferenceMonthDesc(
            vehicleId, referenceMonth);
        if (previous.isPresent()) {
            var closing = previous.get();
            return new Reading(
                closing.getFinalOdometer(),
                closing.getReferenceMonth().withDayOfMonth(
                    YearMonth.from(closing.getReferenceMonth()).lengthOfMonth()).atTime(LocalTime.MAX),
                OdometerOrigin.PREVIOUS_MONTH_CLOSING,
                closing.getId()
            );
        }

        Optional<OperationalDay> firstDay = monthlyDays.stream()
            .filter(day -> day.getStatus() != OperationalDayStatus.CANCELLED)
            .findFirst();
        if (firstDay.isPresent()) {
            OperationalDay day = firstDay.get();
            return new Reading(
                day.getInitialOdometer(), day.getDate().atStartOfDay(),
                OdometerOrigin.FIRST_OPERATIONAL_DAY, day.getId());
        }

        Optional<Shift> firstShift = monthlyShifts.stream()
            .filter(shift -> shift.getStatus() != ShiftStatus.CANCELLED)
            .findFirst();
        if (firstShift.isPresent()) {
            Shift shift = firstShift.get();
            return new Reading(
                shift.getInitialOdometer(), shift.getStartedAt(),
                OdometerOrigin.FIRST_SHIFT, shift.getId());
        }

        if (!monthlyFuelings.isEmpty()) {
            Fueling fueling = monthlyFuelings.getFirst();
            return new Reading(
                fueling.getOdometer(), fueling.getFueledAt(),
                OdometerOrigin.FIRST_FUELING, fueling.getId());
        }

        if (vehicle.getInitialOdometer() != null) {
            return new Reading(
                vehicle.getInitialOdometer(), vehicle.getCreatedAt(),
                OdometerOrigin.VEHICLE_INITIAL, vehicle.getId());
        }
        return null;
    }

    private Reading inferFinal(
        Vehicle vehicle,
        LocalDateTime start,
        LocalDateTime exclusiveEnd,
        List<OperationalDay> monthlyDays,
        List<Shift> monthlyShifts,
        List<Fueling> monthlyFuelings
    ) {
        List<Reading> candidates = finalReadings(monthlyDays, monthlyShifts, monthlyFuelings);

        Set<UUID> representedSources = new HashSet<>();
        candidates.stream().map(Reading::sourceId).filter(java.util.Objects::nonNull)
            .forEach(representedSources::add);
        LocalDateTime vehicleReadingTime = vehicle.getCurrentOdometerRecordedAt();
        boolean readingInsideMonth = vehicleReadingTime != null
            && !vehicleReadingTime.isBefore(start)
            && vehicleReadingTime.isBefore(exclusiveEnd);
        boolean alreadyRepresented = vehicle.getCurrentOdometerSourceId() != null
            && representedSources.contains(vehicle.getCurrentOdometerSourceId());
        if (readingInsideMonth
            && (!alreadyRepresented || vehicle.getCurrentOdometerSource() == OdometerReadingSource.VEHICLE_MANUAL)) {
            candidates.add(new Reading(
                vehicle.getCurrentOdometer(), vehicleReadingTime,
                OdometerOrigin.CURRENT_VEHICLE, vehicle.getCurrentOdometerSourceId()));
        }

        return candidates.stream().max(Comparator.comparing(Reading::recordedAt)).orElse(null);
    }

    private List<Reading> finalReadings(
        List<OperationalDay> monthlyDays,
        List<Shift> monthlyShifts,
        List<Fueling> monthlyFuelings
    ) {
        List<Reading> readings = new ArrayList<>();
        monthlyDays.stream()
            .filter(day -> day.getStatus() == OperationalDayStatus.CLOSED)
            .filter(day -> day.getFinalOdometer() != null)
            .map(day -> new Reading(
                day.getFinalOdometer(), day.getDate().atTime(LocalTime.MAX),
                OdometerOrigin.CLOSED_OPERATIONAL_DAY, day.getId()))
            .forEach(readings::add);
        monthlyShifts.stream()
            .filter(shift -> shift.getStatus() == ShiftStatus.CLOSED)
            .filter(shift -> shift.getFinalOdometer() != null && shift.getEndedAt() != null)
            .map(shift -> new Reading(
                shift.getFinalOdometer(), shift.getEndedAt(),
                OdometerOrigin.CLOSED_SHIFT, shift.getId()))
            .forEach(readings::add);
        monthlyFuelings.stream()
            .map(fueling -> new Reading(
                fueling.getOdometer(), fueling.getFueledAt(),
                OdometerOrigin.FUELING, fueling.getId()))
            .forEach(readings::add);
        return readings;
    }

    private void addChronologicalRegressionAlert(
        Reading initial,
        Vehicle vehicle,
        List<OperationalDay> monthlyDays,
        List<Shift> monthlyShifts,
        List<Fueling> monthlyFuelings,
        List<MileageAlert> alerts
    ) {
        List<Reading> timeline = new ArrayList<>();
        if (initial != null) {
            timeline.add(initial);
        }
        monthlyDays.stream()
            .filter(day -> day.getStatus() != OperationalDayStatus.CANCELLED)
            .map(day -> new Reading(
                day.getInitialOdometer(), day.getDate().atStartOfDay(),
                OdometerOrigin.FIRST_OPERATIONAL_DAY, day.getId()))
            .forEach(timeline::add);
        monthlyShifts.stream()
            .filter(shift -> shift.getStatus() != ShiftStatus.CANCELLED)
            .map(shift -> new Reading(
                shift.getInitialOdometer(), shift.getStartedAt(),
                OdometerOrigin.FIRST_SHIFT, shift.getId()))
            .forEach(timeline::add);
        timeline.addAll(finalReadings(monthlyDays, monthlyShifts, monthlyFuelings));

        LocalDateTime currentTime = vehicle.getCurrentOdometerRecordedAt();
        if (currentTime != null) {
            timeline.add(new Reading(
                vehicle.getCurrentOdometer(), currentTime,
                OdometerOrigin.CURRENT_VEHICLE, vehicle.getCurrentOdometerSourceId()));
        }

        timeline.sort(Comparator.comparing(Reading::recordedAt).thenComparing(Reading::value));
        for (int index = 1; index < timeline.size(); index++) {
            Reading previous = timeline.get(index - 1);
            Reading current = timeline.get(index);
            if (current.recordedAt().isAfter(previous.recordedAt())
                && current.value().compareTo(previous.value()) < 0) {
                addBlockingOnce(
                    alerts,
                    "ODOMETER_REGRESSION",
                    "Há regressão cronológica de odômetro entre "
                        + previous.recordedAt() + " (" + previous.value() + " km) e "
                        + current.recordedAt() + " (" + current.value() + " km)."
                );
                return;
            }
        }
    }

    private void addDayShiftGapWarnings(
        List<OperationalDay> monthlyDays,
        List<Shift> monthlyShifts,
        List<MileageAlert> alerts
    ) {
        monthlyDays.stream()
            .filter(day -> day.getStatus() == OperationalDayStatus.CLOSED)
            .filter(day -> day.getFinalOdometer() != null)
            .forEach(day -> {
                BigDecimal dayDistance = day.getFinalOdometer().subtract(day.getInitialOdometer());
                BigDecimal shiftDistance = monthlyShifts.stream()
                    .filter(shift -> shift.getOperationalDay().getId().equals(day.getId()))
                    .filter(shift -> shift.getStatus() == ShiftStatus.CLOSED)
                    .map(Shift::getDistance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal gap = dayDistance.subtract(shiftDistance).abs();
                if (gap.compareTo(DISTANCE_GAP_TOLERANCE) > 0) {
                    alerts.add(warning(
                        "DAY_SHIFT_DISTANCE_GAP",
                        "Em " + day.getDate() + ", a distância do dia difere dos turnos em "
                            + distance(gap) + " km."
                    ));
                }
            });
    }

    private static void addBlockingOnce(List<MileageAlert> alerts, String code, String message) {
        if (alerts.stream().noneMatch(alert -> alert.code().equals(code))) {
            alerts.add(blocking(code, message));
        }
    }

    private static BigDecimal distance(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP);
    }

    private static MileageAlert blocking(String code, String message) {
        return new MileageAlert(code, MileageAlertSeverity.BLOCKING, message);
    }

    private static MileageAlert warning(String code, String message) {
        return new MileageAlert(code, MileageAlertSeverity.WARNING, message);
    }

    private record Reading(
        BigDecimal value,
        LocalDateTime recordedAt,
        OdometerOrigin origin,
        UUID sourceId
    ) {}
}
