package dev.harrison.rendacomcarro.goal.web;

import dev.harrison.rendacomcarro.goal.application.GoalFormSubmissionService;
import dev.harrison.rendacomcarro.goal.application.GoalService;
import dev.harrison.rendacomcarro.goal.domain.MonthlyGoal;
import dev.harrison.rendacomcarro.goal.domain.WorkloadPeriodicity;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import dev.harrison.rendacomcarro.vehicle.domain.VehicleStatus;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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
@RequestMapping("/goals")
public class GoalController {
    private final GoalService service;
    private final GoalFormSubmissionService submissions;
    private final VehicleService vehicles;

    public GoalController(
        GoalService service,
        GoalFormSubmissionService submissions,
        VehicleService vehicles
    ) {
        this.service = service;
        this.submissions = submissions;
        this.vehicles = vehicles;
    }

    @ModelAttribute("workloadPeriodicities")
    public WorkloadPeriodicity[] workloadPeriodicities() {
        return WorkloadPeriodicity.values();
    }

    @GetMapping
    public String detail(Model model) {
        YearMonth month = YearMonth.now();
        var goal = service.find(month);
        if (goal.isEmpty()) {
            return "redirect:/goals/new";
        }
        var entity = goal.orElseThrow();
        model.addAttribute("goal", entity);
        model.addAttribute("plannedDays", service.plannedDays(entity.getId()));
        model.addAttribute("projection", service.project(
            month,
            entity.getPersonalNetGoal(),
            BigDecimal.ZERO,
            service.plannedDays(entity.getId()).stream()
                .map(day -> day.getWorkDate())
                .collect(Collectors.toSet())
        ));
        return "goals/detail";
    }

    @GetMapping("/new")
    public String form(
        @RequestParam(required = false)
        @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
        Model model
    ) {
        GoalForm form = model.containsAttribute("goalForm")
            ? (GoalForm) model.getAttribute("goalForm")
            : new GoalForm();
        if (month != null) {
            form.setMonth(month);
        }
        List<Vehicle> activeVehicles = activeVehicles();
        if (form.getVehicleIds().isEmpty()) {
            activeVehicles.stream()
                .filter(Vehicle::isPrimaryVehicle)
                .findFirst()
                .ifPresent(vehicle -> form.setVehicleIds(
                    new LinkedHashSet<>(java.util.Set.of(vehicle.getId()))
                ));
        }
        model.addAttribute("goalForm", form);
        model.addAttribute("vehicles", activeVehicles);
        model.addAttribute("editing", false);
        return "goals/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable UUID id, Model model) {
        MonthlyGoal goal = service.get(id);
        GoalForm form = new GoalForm();
        form.setMonth(goal.getMonth());
        form.setPersonalNetGoal(goal.getPersonalNetGoal());
        form.setOperationalGoal(goal.getOperationalGoal());
        form.setVehicleIds(goal.getVehicles().stream()
            .map(Vehicle::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new)));
        form.setWorkloadPeriodicity(goal.getWorkloadPeriodicity());
        form.setWorkloadHours(goal.getEnteredHours());
        form.setWorkloadMinutes(goal.getEnteredRemainderMinutes());
        form.setPlannedDates(service.plannedDays(id).stream()
            .map(day -> day.getWorkDate().toString())
            .collect(Collectors.joining(",")));

        model.addAttribute("goalForm", form);
        model.addAttribute("goal", goal);
        model.addAttribute("vehicles", activeVehicles());
        model.addAttribute("editing", true);
        return "goals/form";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("goalForm") GoalForm form,
        BindingResult result,
        RedirectAttributes redirect,
        Authentication authentication,
        Model model
    ) {
        if (!result.hasErrors()) {
            try {
                submissions.submit(authentication.getName(), form);
            } catch (IllegalArgumentException exception) {
                mapSubmissionError(result, exception);
            }
        }
        if (result.hasErrors()) {
            model.addAttribute("vehicles", activeVehicles());
            model.addAttribute("editing", false);
            return "goals/form";
        }
        redirect.addFlashAttribute("successMessage", "Meta mensal cadastrada.");
        return "redirect:/goals";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable UUID id,
        @Valid @ModelAttribute("goalForm") GoalForm form,
        BindingResult result,
        RedirectAttributes redirect,
        Authentication authentication,
        Model model
    ) {
        MonthlyGoal goal = service.get(id);
        if (!result.hasErrors()) {
            try {
                submissions.update(authentication.getName(), id, form);
            } catch (IllegalArgumentException exception) {
                mapSubmissionError(result, exception);
            }
        }
        if (result.hasErrors()) {
            model.addAttribute("goal", goal);
            model.addAttribute("vehicles", activeVehicles());
            model.addAttribute("editing", true);
            return "goals/form";
        }
        redirect.addFlashAttribute("successMessage", "Meta mensal atualizada.");
        return "redirect:/goals";
    }

    private List<Vehicle> activeVehicles() {
        return vehicles.listAll().stream()
            .filter(vehicle -> vehicle.getStatus() == VehicleStatus.ACTIVE)
            .toList();
    }

    private void mapSubmissionError(BindingResult result, IllegalArgumentException exception) {
        String message = exception.getMessage() == null
            ? "Não foi possível salvar a meta."
            : exception.getMessage();
        if (message.contains("veículo") || message.contains("Veículo")) {
            result.rejectValue("vehicleIds", "invalid", message);
        } else if (message.contains("data")
            || message.contains("Domingos")
            || message.contains("dia planejado")
            || message.contains("dias planejados")
            || message.contains("mês da meta")) {
            result.rejectValue("plannedDates", "invalid", message);
        } else if (message.contains("minutos")) {
            result.rejectValue("workloadMinutes", "invalid", message);
        } else if (message.contains("periodicidade") || message.contains("jornada")) {
            result.rejectValue("workloadPeriodicity", "invalid", message);
        } else if (message.contains("duração") || message.contains("horas")) {
            result.rejectValue("workloadHours", "invalid", message);
        } else {
            result.reject("goal", message);
        }
    }
}
