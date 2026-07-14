package dev.harrison.rendacomcarro.goal.web;

import dev.harrison.rendacomcarro.goal.application.GoalFormSubmissionService;
import dev.harrison.rendacomcarro.goal.application.GoalService;
import dev.harrison.rendacomcarro.goal.domain.WorkloadPeriodicity;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/goals")
public class GoalController {
    private final GoalService service;
    private final GoalFormSubmissionService submissions;

    public GoalController(GoalService service, GoalFormSubmissionService submissions) {
        this.service = service;
        this.submissions = submissions;
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
        model.addAttribute("goalForm", form);
        return "goals/form";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("goalForm") GoalForm form,
        BindingResult result,
        RedirectAttributes redirect,
        Authentication authentication
    ) {
        if (!result.hasErrors()) {
            try {
                submissions.submit(authentication.getName(), form);
            } catch (IllegalArgumentException exception) {
                mapSubmissionError(result, exception);
            }
        }
        if (result.hasErrors()) {
            return "goals/form";
        }
        redirect.addFlashAttribute("successMessage", "Meta mensal cadastrada.");
        return "redirect:/goals";
    }

    private void mapSubmissionError(BindingResult result, IllegalArgumentException exception) {
        String message = exception.getMessage() == null
            ? "Não foi possível salvar a meta."
            : exception.getMessage();
        if (message.contains("data")
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
