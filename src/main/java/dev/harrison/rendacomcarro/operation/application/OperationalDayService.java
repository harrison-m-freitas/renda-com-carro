package dev.harrison.rendacomcarro.operation.application;

import dev.harrison.rendacomcarro.operation.domain.OperationalDay;
import dev.harrison.rendacomcarro.operation.domain.OperationalDayStatus;
import dev.harrison.rendacomcarro.operation.domain.ShiftStatus;
import dev.harrison.rendacomcarro.operation.infrastructure.OperationalDayRepository;
import dev.harrison.rendacomcarro.operation.infrastructure.ShiftRepository;
import dev.harrison.rendacomcarro.shared.domain.DomainConflictException;
import dev.harrison.rendacomcarro.vehicle.application.VehicleOdometerService;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.OdometerReadingSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationalDayService {
    private final OperationalDayRepository days;
    private final ShiftRepository shifts;
    private final VehicleService vehicles;
    private final VehicleOdometerService odometerService;

    public OperationalDayService(
        OperationalDayRepository days,
        ShiftRepository shifts,
        VehicleService vehicles,
        VehicleOdometerService odometerService
    ) {
        this.days = days;
        this.shifts = shifts;
        this.vehicles = vehicles;
        this.odometerService = odometerService;
    }

    @Transactional
    public OperationalDay openDay(LocalDate date, UUID vehicleId, BigDecimal goal, BigDecimal odometer) {
        if (days.existsByVehicleIdAndDateAndStatusNot(vehicleId, date, OperationalDayStatus.CANCELLED)) {
            throw new DomainConflictException("Já existe dia operacional para este veículo e data");
        }
        return days.save(OperationalDay.open(date, vehicles.get(vehicleId), goal, odometer));
    }

    @Transactional
    public OperationalDay closeDay(UUID id, BigDecimal odometer) {
        OperationalDay day = get(id);
        if (shifts.existsByOperationalDayIdAndStatus(id, ShiftStatus.OPEN)) {
            throw new DomainConflictException("Não é possível fechar o dia com turno aberto");
        }
        day.close(odometer);
        OperationalDay saved = days.save(day);
        odometerService.registerReading(
            saved.getVehicle().getId(),
            saved.getFinalOdometer(),
            odometerRecordedAt(saved),
            OdometerReadingSource.OPERATIONAL_DAY_CLOSE,
            saved.getId()
        );
        return saved;
    }

    @Transactional
    public void cancelDay(UUID id) {
        OperationalDay day = get(id);
        if (shifts.existsByOperationalDayIdAndStatus(id, ShiftStatus.OPEN)) {
            throw new DomainConflictException("Encerre o turno aberto antes de cancelar o dia");
        }
        day.cancel();
        days.save(day);
    }

    @Transactional(readOnly = true)
    public OperationalDay get(UUID id) {
        return days.findById(id).orElseThrow(() -> new IllegalArgumentException("Dia operacional não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<OperationalDay> listAll() {
        return days.findAllByOrderByDateDesc();
    }

    private static LocalDateTime odometerRecordedAt(OperationalDay day) {
        LocalDate today = LocalDate.now();
        if (day.getDate().isEqual(today)) {
            return LocalDateTime.now();
        }
        return day.getDate().atTime(LocalTime.MAX);
    }
}
