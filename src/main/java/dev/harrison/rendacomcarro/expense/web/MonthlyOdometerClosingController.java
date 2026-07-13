package dev.harrison.rendacomcarro.expense.web;

import dev.harrison.rendacomcarro.expense.application.MonthlyOdometerClosingService;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import jakarta.validation.Valid;
import java.time.YearMonth;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mileage-closings")
public class MonthlyOdometerClosingController {
    private final MonthlyOdometerClosingService service;
    private final VehicleService vehicles;

    public MonthlyOdometerClosingController(
        MonthlyOdometerClosingService service,
        VehicleService vehicles
    ) {
        this.service = service;
        this.vehicles = vehicles;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("closings", service.listAll());
        return "mileage-closings/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        if (!model.containsAttribute("closingForm")) {
            MonthlyOdometerClosingForm form = new MonthlyOdometerClosingForm();
            form.setMonth(YearMonth.now());
            try {
                var primary = vehicles.getPrimaryVehicle();
                form.setVehicleId(primary.getId());
                form.setInitialOdometer(primary.getCurrentOdometer());
            } catch (IllegalStateException ignored) {
                // O usuário poderá selecionar outro veículo no formulário.
            }
            model.addAttribute("closingForm", form);
        }
        model.addAttribute("vehicles", vehicles.listAll());
        return "mileage-closings/form";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("closingForm") MonthlyOdometerClosingForm form,
        BindingResult result,
        Model model,
        RedirectAttributes redirect
    ) {
        if (result.hasErrors()) {
            model.addAttribute("vehicles", vehicles.listAll());
            return "mileage-closings/form";
        }

        service.create(new MonthlyOdometerClosingService.CreateCommand(
            form.getVehicleId(),
            form.getMonth(),
            form.getInitialOdometer(),
            form.getFinalOdometer(),
            form.getProfessionalKilometers(),
            form.getAdjustmentReason()
        ));
        redirect.addFlashAttribute(
            "successMessage",
            "Fechamento de quilometragem registrado."
        );
        return "redirect:/mileage-closings";
    }
}
