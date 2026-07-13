package dev.harrison.rendacomcarro.fuel;

import dev.harrison.rendacomcarro.fuel.application.FuelingService;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties={"APP_ADMIN_USERNAME=fuel-owner","APP_ADMIN_PASSWORD=fuel-owner-credential"})
class FuelingFlowTest extends PostgresIntegrationTest {
 @Autowired VehicleService vehicles; @Autowired FuelingService service;
 @Test void recordsFueling(){
  var v=vehicles.create(new VehicleService.CreateVehicleCommand("Carro combustível","VW","Voyage",2018,"FUL1A23",FuelType.FLEX,new BigDecimal("50000.0"),new BigDecimal("40000.00")));
  var f=service.create(new FuelingService.CreateFuelingCommand(v.getId(),LocalDateTime.now(),new BigDecimal("50000.0"),"Posto",FuelType.FLEX,new BigDecimal("20.000"),new BigDecimal("6.000"),new BigDecimal("120.00"),true,null));
  assertThat(f.getTotalAmount()).isEqualByComparingTo("120.00");
 }
}
