package dev.harrison.rendacomcarro.finance.web;

import dev.harrison.rendacomcarro.finance.application.AcquisitionPlanService;
import dev.harrison.rendacomcarro.shared.web.BrazilianBigDecimalEditor;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
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
@RequestMapping("/acquisition-plans")
public class AcquisitionPlanController {
    private final AcquisitionPlanService plans;
    private final VehicleService vehicles;

    public AcquisitionPlanController(AcquisitionPlanService plans, VehicleService vehicles) {
        this.plans = plans;
        this.vehicles = vehicles;
    }

    @InitBinder("acquisitionPlanForm")
    void bindFinancialValues(WebDataBinder binder) {
        binder.registerCustomEditor(
            BigDecimal.class, "purchaseAmount", new BrazilianBigDecimalEditor()
        );
        binder.registerCustomEditor(
            BigDecimal.class, "ownResourcesAmount", new BrazilianBigDecimalEditor()
        );
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("plans", plans.list());
        return "acquisition-plans/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        if (!model.containsAttribute("acquisitionPlanForm")) {
            model.addAttribute("acquisitionPlanForm", new AcquisitionPlanForm());
        }
        model.addAttribute("vehicles", vehicles.listAll());
        return "acquisition-plans/form";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("acquisitionPlanForm") AcquisitionPlanForm form,
        BindingResult result,
        Model model,
        RedirectAttributes redirect
    ) {
        if (form.getPurchaseAmount() != null && form.getOwnResourcesAmount() != null
            && form.getOwnResourcesAmount().compareTo(form.getPurchaseAmount()) > 0) {
            result.rejectValue(
                "ownResourcesAmount", "maximum",
                "Os recursos próprios não podem ultrapassar o valor da compra"
            );
        }
        if (result.hasErrors()) {
            model.addAttribute("vehicles", vehicles.listAll());
            return "acquisition-plans/form";
        }
        var plan = plans.create(new AcquisitionPlanService.CreateCommand(
            form.getVehicleId(), form.getTitle(), form.getPurchaseAmount(),
            form.getOwnResourcesAmount(), form.getPurchaseDate(), form.getNotes()
        ));
        redirect.addFlashAttribute("successMessage", "Plano de compra criado.");
        return "redirect:/acquisition-plans/" + plan.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("summary", plans.summary(id));
        return "acquisition-plans/detail";
    }
}
