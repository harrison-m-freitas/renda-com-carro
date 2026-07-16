package dev.harrison.rendacomcarro.vehicle.web;

import dev.harrison.rendacomcarro.shared.web.BrazilianBigDecimalEditor;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.FuelType;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vehicles")
public class VehicleController {
    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @InitBinder("vehicleForm")
    void bindBrazilianVehicleNumbers(WebDataBinder binder) {
        binder.registerCustomEditor(
            BigDecimal.class,
            "initialOdometer",
            new BrazilianBigDecimalEditor()
        );
        binder.registerCustomEditor(
            BigDecimal.class,
            "purchasePrice",
            new BrazilianBigDecimalEditor()
        );
    }

    @ModelAttribute("fuelTypes")
    FuelType[] fuelTypes() {
        return FuelType.values();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("vehicles", service.listAll());
        return "vehicles/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("vehicleForm")) {
            model.addAttribute("vehicleForm", new VehicleForm());
        }
        model.addAttribute("editing", false);
        return "vehicles/form";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("vehicleForm") VehicleForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", false);
            return "vehicles/form";
        }

        service.create(new VehicleService.CreateVehicleCommand(
            form.getName(),
            form.getMake(),
            form.getModel(),
            form.getYear(),
            form.getPlate(),
            form.getFuelType(),
            form.getInitialOdometer(),
            form.getPurchasePrice()
        ));
        redirectAttributes.addFlashAttribute("successMessage", "Veículo cadastrado.");
        return "redirect:/vehicles";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("vehicle", service.get(id));
        return "vehicles/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        Vehicle vehicle = service.get(id);
        if (!model.containsAttribute("vehicleForm")) {
            VehicleForm form = new VehicleForm();
            form.setName(vehicle.getName());
            form.setMake(vehicle.getMake());
            form.setModel(vehicle.getModel());
            form.setYear(vehicle.getYear());
            form.setPlate(vehicle.getPlate());
            form.setFuelType(vehicle.getFuelType());
            form.setInitialOdometer(vehicle.getCurrentOdometer());
            form.setPurchasePrice(vehicle.getPurchasePrice());
            model.addAttribute("vehicleForm", form);
        }
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("editing", true);
        return "vehicles/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable UUID id,
        @Valid @ModelAttribute("vehicleForm") VehicleForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("vehicle", service.get(id));
            model.addAttribute("editing", true);
            return "vehicles/form";
        }

        service.update(id, new VehicleService.UpdateVehicleCommand(
            form.getName(),
            form.getMake(),
            form.getModel(),
            form.getYear(),
            form.getPlate(),
            form.getFuelType(),
            form.getInitialOdometer(),
            form.getPurchasePrice()
        ));
        redirectAttributes.addFlashAttribute("successMessage", "Veículo atualizado.");
        return "redirect:/vehicles/" + id;
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        service.activate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Veículo ativado. O veículo anterior foi inativado.");
        return "redirect:/vehicles";
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            service.archive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Veículo arquivado.");
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/vehicles";
    }
}
