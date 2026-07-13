package dev.harrison.rendacomcarro.fuel;

import dev.harrison.rendacomcarro.fuel.application.FuelCostCalculator;
import dev.harrison.rendacomcarro.fuel.domain.Fueling;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FuelCostCalculatorTest {
 private final FuelCostCalculator calculator=new FuelCostCalculator();
 @Test void estimatesShiftFuelCost(){assertThat(calculator.estimateCost(new BigDecimal("164.0"),new BigDecimal("10.5"),new BigDecimal("6.19"))).isEqualByComparingTo("96.68");}
 @Test void doesNotCalculateConsumptionAcrossPartialFillings(){
  Vehicle v=Vehicle.create("Teste","Fiat","Uno",2015,"FUE1A23",FuelType.FLEX,BigDecimal.ZERO,new BigDecimal("30000"));
  Fueling previous=Fueling.create(v,LocalDateTime.now().minusDays(1),new BigDecimal("1000.0"),null,FuelType.FLEX,new BigDecimal("40.000"),new BigDecimal("6.000"),new BigDecimal("240.00"),true,null);
  Fueling current=Fueling.create(v,LocalDateTime.now(),new BigDecimal("1200.0"),null,FuelType.FLEX,new BigDecimal("20.000"),new BigDecimal("6.000"),new BigDecimal("120.00"),false,null);
  assertThat(calculator.consumptionBetween(previous,current)).isEmpty();
 }
}
