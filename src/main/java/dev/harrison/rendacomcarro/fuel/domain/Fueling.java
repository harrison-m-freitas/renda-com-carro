package dev.harrison.rendacomcarro.fuel.domain;

import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="fueling")
public class Fueling {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id") private Vehicle vehicle;
 @Column(name="fueled_at",nullable=false) private LocalDateTime fueledAt;
 @Column(nullable=false,precision=12,scale=1) private BigDecimal odometer;
 @Column private String station;
 @Enumerated(EnumType.STRING) @Column(name="fuel_type",nullable=false) private FuelType fuelType;
 @Column(nullable=false,precision=12,scale=3) private BigDecimal liters;
 @Column(name="price_per_liter",nullable=false,precision=12,scale=3) private BigDecimal pricePerLiter;
 @Column(name="total_amount",nullable=false,precision=14,scale=2) private BigDecimal totalAmount;
 @Column(name="full_tank",nullable=false) private boolean fullTank;
 @Column(columnDefinition="text") private String notes;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 protected Fueling(){}
 public static Fueling create(Vehicle vehicle,LocalDateTime fueledAt,BigDecimal odometer,String station,FuelType fuelType,BigDecimal liters,BigDecimal pricePerLiter,BigDecimal totalAmount,boolean fullTank,String notes){
  if(vehicle==null||fueledAt==null||fuelType==null) throw new IllegalArgumentException("Veículo, data e combustível são obrigatórios");
  if(odometer==null||odometer.signum()<0) throw new IllegalArgumentException("Odômetro inválido");
  if(liters==null||liters.signum()<=0||pricePerLiter==null||pricePerLiter.signum()<=0||totalAmount==null||totalAmount.signum()<=0) throw new IllegalArgumentException("Valores do abastecimento devem ser positivos");
  BigDecimal calculated=liters.multiply(pricePerLiter).setScale(2,java.math.RoundingMode.HALF_UP);
  if(calculated.subtract(totalAmount).abs().compareTo(new BigDecimal("0.05"))>0) throw new IllegalArgumentException("Total incompatível com litros e preço");
  Fueling f=new Fueling();f.id=UUID.randomUUID();f.vehicle=vehicle;f.fueledAt=fueledAt;f.odometer=DecimalPolicy.distance(odometer);f.station=station;f.fuelType=fuelType;f.liters=DecimalPolicy.volume(liters);f.pricePerLiter=DecimalPolicy.volume(pricePerLiter);f.totalAmount=DecimalPolicy.money(totalAmount);f.fullTank=fullTank;f.notes=notes;f.createdAt=LocalDateTime.now();return f;
 }
 public UUID getId(){return id;} public Vehicle getVehicle(){return vehicle;} public LocalDateTime getFueledAt(){return fueledAt;} public BigDecimal getOdometer(){return odometer;} public String getStation(){return station;} public FuelType getFuelType(){return fuelType;} public BigDecimal getLiters(){return liters;} public BigDecimal getPricePerLiter(){return pricePerLiter;} public BigDecimal getTotalAmount(){return totalAmount;} public boolean isFullTank(){return fullTank;} public String getNotes(){return notes;}
}
