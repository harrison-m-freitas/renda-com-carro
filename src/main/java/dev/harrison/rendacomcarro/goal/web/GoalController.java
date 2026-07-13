package dev.harrison.rendacomcarro.goal.web;

import dev.harrison.rendacomcarro.goal.application.GoalService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/goals")
public class GoalController {
    private final GoalService service;
    public GoalController(GoalService service){this.service=service;}

    @GetMapping
    public String detail(Model model){
        YearMonth month=YearMonth.now();
        var goal=service.find(month);
        if(goal.isEmpty()) return "redirect:/goals/new";
        var entity=goal.orElseThrow();
        model.addAttribute("goal",entity);
        model.addAttribute("plannedDays",service.plannedDays(entity.getId()));
        model.addAttribute("projection",service.project(month,entity.getPersonalNetGoal(),BigDecimal.ZERO,
            service.plannedDays(entity.getId()).stream().map(d->d.getWorkDate()).collect(Collectors.toSet())));
        return "goals/detail";
    }

    @GetMapping("/new")
    public String form(Model model){if(!model.containsAttribute("goalForm"))model.addAttribute("goalForm",new GoalForm());return "goals/form";}

    @PostMapping
    public String create(@Valid @ModelAttribute("goalForm") GoalForm form,BindingResult result,RedirectAttributes redirect){
        Set<LocalDate> dates;
        try { dates=Arrays.stream(form.getPlannedDates().split("[,;\\s]+"))
            .filter(s->!s.isBlank()).map(LocalDate::parse).collect(Collectors.toSet()); }
        catch(Exception e){result.rejectValue("plannedDates","invalid","Use datas no formato AAAA-MM-DD.");dates=Set.of();}
        if(result.hasErrors()) return "goals/form";
        service.create(form.getMonth(),form.getPersonalNetGoal(),form.getOperationalGoal(),form.getPlannedHours(),dates);
        redirect.addFlashAttribute("successMessage","Meta mensal cadastrada."); return "redirect:/goals";
    }
}
