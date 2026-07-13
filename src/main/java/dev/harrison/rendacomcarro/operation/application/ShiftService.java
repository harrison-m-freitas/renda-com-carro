package dev.harrison.rendacomcarro.operation.application;

import dev.harrison.rendacomcarro.operation.domain.OperationalDay;
import dev.harrison.rendacomcarro.operation.domain.OperationalDayStatus;
import dev.harrison.rendacomcarro.operation.domain.Platform;
import dev.harrison.rendacomcarro.operation.domain.Shift;
import dev.harrison.rendacomcarro.operation.domain.ShiftStatus;
import dev.harrison.rendacomcarro.operation.infrastructure.PlatformRepository;
import dev.harrison.rendacomcarro.operation.infrastructure.ShiftRepository;
import dev.harrison.rendacomcarro.shared.domain.DomainConflictException;
import dev.harrison.rendacomcarro.vehicle.application.VehicleOdometerService;
import dev.harrison.rendacomcarro.vehicle.domain.OdometerReadingSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService {
    private final ShiftRepository shifts;
    private final PlatformRepository platforms;
    private final OperationalDayService days;
    private final VehicleOdometerService odometerService;

    public ShiftService(
        ShiftRepository shifts,
        PlatformRepository platforms,
        OperationalDayService days,
        VehicleOdometerService odometerService
    ) {
        this.shifts = shifts;
        this.platforms = platforms;
        this.days = days;
        this.odometerService = odometerService;
    }

    @Transactional
    public Shift openShift(UUID dayId, LocalDateTime start, BigDecimal odometer, String region, Set<UUID> platformIds) {
        OperationalDay day = days.get(dayId);
        if (day.getStatus() != OperationalDayStatus.IN_PROGRESS) {
            throw new DomainConflictException("Dia operacional não está em andamento");
        }
        if (shifts.existsByOperationalDayIdAndStatus(dayId, ShiftStatus.OPEN)) {
            throw new DomainConflictException("Já existe turno aberto neste dia");
        }
        BigDecimal minimum = shifts.findFirstByOperationalDayIdOrderByStartedAtDesc(dayId)
            .map(shift -> shift.getFinalOdometer() == null ? shift.getInitialOdometer() : shift.getFinalOdometer())
            .orElse(day.getInitialOdometer());
        if (odometer == null || odometer.compareTo(minimum) < 0) {
            throw new IllegalArgumentException("Odômetro inicial não pode ser menor que o último registro");
        }
        if (platformIds == null || platformIds.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos uma plataforma");
        }
        Set<Platform> selected = new LinkedHashSet<>(platforms.findAllById(platformIds));
        if (selected.size() != platformIds.size()) {
            throw new IllegalArgumentException("Plataforma inválida");
        }
        return shifts.save(Shift.open(day, start, odometer, region, selected));
    }

    @Transactional
    public Shift closeShift(UUID id, LocalDateTime end, BigDecimal odometer, String region, Set<String> neighborhoods) {
        Shift shift = get(id);
        shift.close(end, odometer, region, neighborhoods);
        Shift saved = shifts.save(shift);
        odometerService.registerReading(
            saved.getOperationalDay().getVehicle().getId(),
            saved.getFinalOdometer(),
            saved.getEndedAt(),
            OdometerReadingSource.SHIFT_CLOSE,
            saved.getId()
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public Shift get(UUID id) {
        return shifts.findById(id).orElseThrow(() -> new IllegalArgumentException("Turno não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Shift> listByDay(UUID dayId) {
        return shifts.findAllByOperationalDayIdOrderByStartedAtAsc(dayId);
    }
}
