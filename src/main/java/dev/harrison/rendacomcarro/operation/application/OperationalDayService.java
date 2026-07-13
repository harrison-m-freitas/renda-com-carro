package dev.harrison.rendacomcarro.operation.application;
import dev.harrison.rendacomcarro.operation.domain.*; import dev.harrison.rendacomcarro.operation.infrastructure.*; import dev.harrison.rendacomcarro.shared.domain.DomainConflictException; import dev.harrison.rendacomcarro.vehicle.application.VehicleService; import java.math.BigDecimal; import java.time.LocalDate; import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class OperationalDayService {
 private final OperationalDayRepository days; private final ShiftRepository shifts; private final VehicleService vehicles;
 public OperationalDayService(OperationalDayRepository days,ShiftRepository shifts,VehicleService vehicles){this.days=days;this.shifts=shifts;this.vehicles=vehicles;}
 @Transactional public OperationalDay openDay(LocalDate date,UUID vehicleId,BigDecimal goal,BigDecimal odo){ if(days.existsByVehicleIdAndDateAndStatusNot(vehicleId,date,OperationalDayStatus.CANCELLED)) throw new DomainConflictException("Já existe dia operacional para este veículo e data"); return days.save(OperationalDay.open(date,vehicles.get(vehicleId),goal,odo)); }
 @Transactional public OperationalDay closeDay(UUID id,BigDecimal odo){OperationalDay day=get(id); if(shifts.existsByOperationalDayIdAndStatus(id,ShiftStatus.OPEN)) throw new DomainConflictException("Não é possível fechar o dia com turno aberto"); day.close(odo); return days.save(day);}
 @Transactional public void cancelDay(UUID id){OperationalDay day=get(id); if(shifts.existsByOperationalDayIdAndStatus(id,ShiftStatus.OPEN)) throw new DomainConflictException("Encerre o turno aberto antes de cancelar o dia"); day.cancel(); days.save(day);}
 @Transactional(readOnly=true) public OperationalDay get(UUID id){return days.findById(id).orElseThrow(()->new IllegalArgumentException("Dia operacional não encontrado"));}
 @Transactional(readOnly=true) public List<OperationalDay> listAll(){return days.findAllByOrderByDateDesc();}
}
