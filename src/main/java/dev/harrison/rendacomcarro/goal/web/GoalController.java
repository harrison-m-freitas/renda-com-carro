package dev.harrison.rendacomcarro.goal.web;

import dev.harrison.rendacomcarro.goal.application.GoalFormSubmissionService;
import dev.harrison.rendacomcarro.goal.application.GoalMonthLabelFormatter;
import dev.harrison.rendacomcarro.goal.application.GoalService;
import dev.harrison.rendacomcarro.goal.domain.MonthlyGoal;
import dev.harrison.rendacomcarro.goal.domain.WorkloadPeriodicity;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
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
    private final GoalMonthLabelFormatter monthLabels;

    public GoalController(
        GoalService service,
        GoalFormSubmissionService submissions,
        VehicleService vehicles,
        GoalMonthLabelFormatter monthLabels
    ) {
        this.service = service;
        this.submissions = submissions;
        this.vehicles = vehicles;
        this.monthLabels = monthLabels;
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
        var plannedDays = service.plannedDays(entity.getId());
        int dayCount = plannedDays.size();
        long totalMinutes = entity.getCalculatedMonthMinutes();
        model.addAttribute("goal", entity);
        model.addAttribute("monthLabel", monthLabels.format(entity.getMonth()));
        model.addAttribute("plannedDays", plannedDays);
        model.addAttribute("projection", service.project(
            month,
            entity.getPersonalNetGoal(),
            BigDecimal.ZERO,
            plannedDays.stream().map(day -> day.getWorkDate()).collect(Collectors.toSet())
        ));
        model.addAttribute("operationalPerDay", perDay(entity.getOperationalGoal(), dayCount));
        model.addAttribute("operationalPerHour", perHour(entity.getOperationalGoal(), totalMinutes));
        model.addAttribute("personalPerDay", perDay(entity.getPersonalNetGoal(), dayCount));
        model.addAttribute("personalPerHour", perHour(entity.getPersonalNetGoal(), totalMinutes));
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
        model.addAttribute("goalForm", form);
        model.addAttribute("vehicle", vehicles.findActiveVehicle().orElse(null));
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
        form.setWorkloadPeriodicity(goal.getWorkloadPeriodicity());
        form.setWorkloadHours(goal.getEnteredHours());
        form.setWorkloadMinutes(goal.getEnteredRemainderMinutes());
        form.setPlannedDates(service.plannedDays(id).stream()
            .map(day -> day.getWorkDate().toString())
            .collect(Collectors.joining(",")));

        model.addAttribute("goalForm", form);
        model.addAttribute("goal", goal);
        model.addAttribute("vehicle", goal.getVehicle());
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
            model.addAttribute("vehicle", vehicles.findActiveVehicle().orElse(null));
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
            model.addAttribute("vehicle", goal.getVehicle());
            model.addAttribute("editing", true);
            return "goals/form";
        }
        redirect.addFlashAttribute("successMessage", "Meta mensal atualizada.");
        return "redirect:/goals";
    }

    private BigDecimal perDay(BigDecimal value, int dayCount) {
        return dayCount <= 0
            ? BigDecimal.ZERO.setScale(2)
            : value.divide(BigDecimal.valueOf(dayCount), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal perHour(BigDecimal value, long totalMinutes) {
        return totalMinutes <= 0
            ? BigDecimal.ZERO.setScale(2)
            : value.multiply(BigDecimal.valueOf(60))
                .divide(BigDecimal.valueOf(totalMinutes), 2, RoundingMode.HALF_UP);
    }

    private void mapSubmissionError(BindingResult result, IllegalArgumentException exception) {
        String message = exception.getMessage() == null
            ? "Não foi possível salvar a meta."
            : exception.getMessage();
        if (message.contains("veículo") || message.contains("Veículo")) {
            result.reject("vehicle", message);
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
