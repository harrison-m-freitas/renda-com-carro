package dev.harrison.rendacomcarro.expense.web;

import dev.harrison.rendacomcarro.expense.application.ExpenseFormSubmissionService;
import dev.harrison.rendacomcarro.expense.application.ExpenseService;
import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseCategoryRepository;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/expenses")
public class ExpenseController {
    private final ExpenseService service;
    private final ExpenseFormSubmissionService submissions;
    private final ExpenseCategoryRepository categories;
    private final VehicleService vehicles;

    public ExpenseController(
        ExpenseService service,
        ExpenseFormSubmissionService submissions,
        ExpenseCategoryRepository categories,
        VehicleService vehicles
    ) {
        this.service = service;
        this.submissions = submissions;
        this.categories = categories;
        this.vehicles = vehicles;
    }

    @ModelAttribute("classifications")
    ExpenseClassification[] classifications() {
        return ExpenseClassification.values();
    }

    @ModelAttribute("allocationMethods")
    AllocationMethod[] allocationMethods() {
        return AllocationMethod.values();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("expenses", service.listAll());
        return "expenses/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        if (!model.containsAttribute("expenseForm")) {
            model.addAttribute("expenseForm", new ExpenseForm());
        }
        populateReferences(model);
        return "expenses/form";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("expenseForm") ExpenseForm form,
        BindingResult result,
        Model model,
        RedirectAttributes redirect,
        Authentication authentication
    ) {
        if (result.hasErrors()) {
            populateReferences(model);
            return "expenses/form";
        }
        try {
            submissions.submit(authentication.getName(), form);
        } catch (IllegalArgumentException exception) {
            result.reject("expense", exception.getMessage());
            populateReferences(model);
            return "expenses/form";
        }
        redirect.addFlashAttribute("successMessage", "Gasto registrado.");
        return "redirect:/expenses";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable UUID id, RedirectAttributes redirect) {
        service.cancel(id);
        redirect.addFlashAttribute("successMessage", "Gasto cancelado.");
        return "redirect:/expenses";
    }

    private void populateReferences(Model model) {
        model.addAttribute("categories", categories.findAllByActiveTrueOrderByNameAsc());
        model.addAttribute("vehicles", vehicles.listAll());
    }
}
