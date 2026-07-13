package dev.harrison.rendacomcarro.operation.web;

import dev.harrison.rendacomcarro.operation.application.RevenueService;
import dev.harrison.rendacomcarro.operation.application.ShiftService;
import dev.harrison.rendacomcarro.operation.domain.DataSource;
import dev.harrison.rendacomcarro.operation.domain.RevenueType;
import dev.harrison.rendacomcarro.operation.infrastructure.PlatformRepository;
import jakarta.validation.Valid;
import java.util.UUID;
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
@RequestMapping("/shifts/{shiftId}/revenues")
public class RevenueController {
    private final RevenueService revenueService;
    private final ShiftService shiftService;
    private final PlatformRepository platformRepository;

    public RevenueController(RevenueService revenueService, ShiftService shiftService,
                             PlatformRepository platformRepository) {
        this.revenueService = revenueService;
        this.shiftService = shiftService;
        this.platformRepository = platformRepository;
    }

    @ModelAttribute("revenueTypes")
    RevenueType[] revenueTypes() { return RevenueType.values(); }

    @ModelAttribute("sources")
    DataSource[] sources() { return DataSource.values(); }

    @GetMapping("/new")
    public String form(@PathVariable UUID shiftId, Model model) {
        model.addAttribute("shift", shiftService.get(shiftId));
        model.addAttribute("platforms", platformRepository.findAllByActiveTrueOrderByNameAsc());
        if (!model.containsAttribute("revenueForm")) model.addAttribute("revenueForm", new RevenueForm());
        return "revenues/form";
    }

    @PostMapping
    public String create(@PathVariable UUID shiftId,
                         @Valid @ModelAttribute("revenueForm") RevenueForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("shift", shiftService.get(shiftId));
            model.addAttribute("platforms", platformRepository.findAllByActiveTrueOrderByNameAsc());
            return "revenues/form";
        }
        revenueService.create(new RevenueService.CreateRevenueCommand(
            shiftId, null, form.getPlatformId(), form.getType(), form.getCompetenceDate(),
            form.getReceivedDate(), form.getGrossAmount(), form.getPlatformFee(), form.getNetAmount(),
            form.getTipAmount(), form.getBonusAmount(), form.getSource(), form.getExternalReference()
        ));
        redirectAttributes.addFlashAttribute("successMessage", "Receita registrada.");
        return "redirect:/operation-days/" + shiftService.get(shiftId).getOperationalDay().getId();
    }
}
