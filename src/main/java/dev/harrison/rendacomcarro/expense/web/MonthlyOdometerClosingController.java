package dev.harrison.rendacomcarro.expense.web;

import dev.harrison.rendacomcarro.expense.application.MonthlyClosingFormSubmissionService;
import dev.harrison.rendacomcarro.expense.application.MonthlyMileagePreview;
import dev.harrison.rendacomcarro.expense.application.MonthlyOdometerClosingService;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mileage-closings")
public class MonthlyOdometerClosingController {
    private final MonthlyOdometerClosingService service;
    private final MonthlyClosingFormSubmissionService submissions;
    private final VehicleService vehicles;

    public MonthlyOdometerClosingController(
        MonthlyOdometerClosingService service,
        MonthlyClosingFormSubmissionService submissions,
        VehicleService vehicles
    ) {
        this.service = service;
        this.submissions = submissions;
        this.vehicles = vehicles;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("closings", service.listAll());
        return "mileage-closings/list";
    }

    @GetMapping("/new")
    public String form(
        @RequestParam(required = false) UUID vehicleId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
        Model model
    ) {
        MonthlyOdometerClosingForm form = model.containsAttribute("closingForm")
            ? (MonthlyOdometerClosingForm) model.getAttribute("closingForm")
            : new MonthlyOdometerClosingForm();

        if (vehicleId == null) {
            vehicleId = form.getVehicleId();
        }
        if (vehicleId == null) {
            try {
                vehicleId = vehicles.getPrimaryVehicle().getId();
            } catch (IllegalStateException ignored) {
                // O usuário poderá selecionar qualquer veículo ativo.
            }
        }
        if (month == null) {
            month = form.getMonth() == null ? YearMonth.now() : form.getMonth();
        }

        form.setVehicleId(vehicleId);
        form.setMonth(month);
        model.addAttribute("closingForm", form);
        model.addAttribute("vehicles", vehicles.listAll());

        if (vehicleId != null && month != null) {
            populatePreview(model, form, vehicleId, month, !form.isManualAdjustment());
        }
        return "mileage-closings/form";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("closingForm") MonthlyOdometerClosingForm form,
        BindingResult result,
        Model model,
        RedirectAttributes redirect,
        Authentication authentication
    ) {
        if (result.hasErrors()) {
            return showInvalidForm(form, model);
        }

        try {
            var closing = submissions.submit(authentication.getName(), form);
            redirect.addFlashAttribute(
                "successMessage",
                closing.isManualAdjustment()
                    ? "Fechamento salvo com correção manual justificada."
                    : "Fechamento automático confirmado."
            );
            return "redirect:/mileage-closings";
        } catch (IllegalArgumentException | DomainValidationException exception) {
            result.reject("closing", exception.getMessage());
            return showInvalidForm(form, model);
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("closing", service.get(id));
        return "mileage-closings/detail";
    }

    private String showInvalidForm(MonthlyOdometerClosingForm form, Model model) {
        model.addAttribute("vehicles", vehicles.listAll());
        if (form.getVehicleId() != null && form.getMonth() != null) {
            populatePreview(model, form, form.getVehicleId(), form.getMonth(), false);
        }
        return "mileage-closings/form";
    }

    private void populatePreview(
        Model model,
        MonthlyOdometerClosingForm form,
        UUID vehicleId,
        YearMonth month,
        boolean copyInferredValues
    ) {
        MonthlyMileagePreview preview = service.preview(vehicleId, month);
        model.addAttribute("preview", preview);
        if (copyInferredValues) {
            form.setInitialOdometer(preview.inferredInitialOdometer());
            form.setFinalOdometer(preview.inferredFinalOdometer());
            form.setProfessionalKilometers(preview.professionalKilometers());
        }
    }
}
