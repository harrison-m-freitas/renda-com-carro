package dev.harrison.rendacomcarro.expense.web;

import dev.harrison.rendacomcarro.expense.application.ExpenseService;
import dev.harrison.rendacomcarro.expense.domain.*;
import dev.harrison.rendacomcarro.expense.infrastructure.ExpenseCategoryRepository;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/expenses")
public class ExpenseController {
 private final ExpenseService service; private final ExpenseCategoryRepository categories; private final VehicleService vehicles;
 public ExpenseController(ExpenseService service,ExpenseCategoryRepository categories,VehicleService vehicles){this.service=service;this.categories=categories;this.vehicles=vehicles;}
 @ModelAttribute("classifications") ExpenseClassification[] classifications(){return ExpenseClassification.values();}
 @ModelAttribute("allocationMethods") AllocationMethod[] allocationMethods(){return AllocationMethod.values();}
 @GetMapping public String list(Model model){model.addAttribute("expenses",service.listAll());return "expenses/list";}
 @GetMapping("/new") public String form(Model model){if(!model.containsAttribute("expenseForm"))model.addAttribute("expenseForm",new ExpenseForm());model.addAttribute("categories",categories.findAllByActiveTrueOrderByNameAsc());model.addAttribute("vehicles",vehicles.listAll());return "expenses/form";}
 @PostMapping public String create(@Valid @ModelAttribute("expenseForm") ExpenseForm form,BindingResult result,Model model,RedirectAttributes redirect){
  if(result.hasErrors()){model.addAttribute("categories",categories.findAllByActiveTrueOrderByNameAsc());model.addAttribute("vehicles",vehicles.listAll());return "expenses/form";}
  service.create(new ExpenseService.CreateExpenseCommand(form.getVehicleId(),form.getOperationalDayId(),form.getShiftId(),form.getCategoryId(),form.getExpenseDate(),form.getCompetenceDate(),form.getPaidDate(),form.getAmount(),form.getClassification(),form.getAllocationMethod(),form.getProfessionalPercentage(),form.getProfessionalFixedAmount(),form.getAdjustmentReason(),form.getNotes()));
  redirect.addFlashAttribute("successMessage","Gasto registrado.");return "redirect:/expenses";
 }
 @PostMapping("/{id}/cancel") public String cancel(@PathVariable UUID id,RedirectAttributes redirect){service.cancel(id);redirect.addFlashAttribute("successMessage","Gasto cancelado.");return "redirect:/expenses";}
}
