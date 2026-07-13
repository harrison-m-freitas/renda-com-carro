package dev.harrison.rendacomcarro.fuel.web;

import dev.harrison.rendacomcarro.fuel.application.FuelingService;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/fuelings")
public class FuelingController {
 private final FuelingService service; private final VehicleService vehicles;
 public FuelingController(FuelingService service,VehicleService vehicles){this.service=service;this.vehicles=vehicles;}
 @ModelAttribute("fuelTypes") FuelType[] fuelTypes(){return FuelType.values();}
 @GetMapping public String list(Model model){model.addAttribute("fuelings",service.listAll());return "fuelings/list";}
 @GetMapping("/new") public String form(Model model){if(!model.containsAttribute("fuelingForm"))model.addAttribute("fuelingForm",new FuelingForm());model.addAttribute("vehicles",vehicles.listAll());return "fuelings/form";}
 @PostMapping public String create(@Valid @ModelAttribute("fuelingForm") FuelingForm form,BindingResult result,Model model,RedirectAttributes redirect){
  if(result.hasErrors()){model.addAttribute("vehicles",vehicles.listAll());return "fuelings/form";}
  service.create(new FuelingService.CreateFuelingCommand(form.getVehicleId(),form.getFueledAt(),form.getOdometer(),form.getStation(),form.getFuelType(),form.getLiters(),form.getPricePerLiter(),form.getTotalAmount(),form.isFullTank(),form.getNotes()));
  redirect.addFlashAttribute("successMessage","Abastecimento registrado.");return "redirect:/fuelings";
 }
}
