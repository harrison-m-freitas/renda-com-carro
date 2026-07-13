package dev.harrison.rendacomcarro.fuel.application;

import dev.harrison.rendacomcarro.fuel.domain.Fueling;
import dev.harrison.rendacomcarro.fuel.infrastructure.FuelingRepository;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuelingService {
 private final FuelingRepository repository; private final VehicleService vehicles;
 public FuelingService(FuelingRepository repository,VehicleService vehicles){this.repository=repository;this.vehicles=vehicles;}
 public record CreateFuelingCommand(UUID vehicleId,LocalDateTime fueledAt,BigDecimal odometer,String station,FuelType fuelType,BigDecimal liters,BigDecimal pricePerLiter,BigDecimal totalAmount,boolean fullTank,String notes){}
 @Transactional public Fueling create(CreateFuelingCommand c){
  var vehicle=vehicles.get(c.vehicleId());
  repository.findFirstByVehicleIdOrderByOdometerDesc(vehicle.getId()).ifPresent(previous->{if(c.odometer().compareTo(previous.getOdometer())<0)throw new IllegalArgumentException("Odômetro não pode regredir");});
  return repository.save(Fueling.create(vehicle,c.fueledAt(),c.odometer(),c.station(),c.fuelType(),c.liters(),c.pricePerLiter(),c.totalAmount(),c.fullTank(),c.notes()));
 }
 @Transactional(readOnly=true) public List<Fueling> listAll(){return repository.findAllByOrderByFueledAtDesc();}
}
