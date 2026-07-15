package dev.harrison.rendacomcarro.finance.web;

import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.finance.application.FinancialObligationService;
import dev.harrison.rendacomcarro.finance.application.ObligationFormSubmissionService;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.domain.ObligationType;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/obligations")
public class FinancialObligationController {
    private final FinancialObligationService service;
    private final ObligationFormSubmissionService submissions;
    private final FormDraftService drafts;
    private final VehicleService vehicles;

    public FinancialObligationController(
        FinancialObligationService service,
        ObligationFormSubmissionService submissions,
        FormDraftService drafts,
        VehicleService vehicles
    ) {
        this.service = service;
        this.submissions = submissions;
        this.drafts = drafts;
        this.vehicles = vehicles;
    }

    @ModelAttribute("types")
    ObligationType[] types() {
        return ObligationType.values();
    }

    @ModelAttribute("modes")
    ObligationMode[] modes() {
        return ObligationMode.values();
    }

    @GetMapping
    public String list(Model model, Authentication authentication) {
        model.addAttribute("obligations", service.list());
        model.addAttribute(
            "obligationDrafts",
            drafts.listActive(authentication.getName(), FormDraftType.OBLIGATION)
                .stream()
                .map(this::toCard)
                .toList()
        );
        return "obligations/list";
    }

    @GetMapping("/new")
    public String form(
        @RequestParam(required = false) String draftKey,
        @RequestParam(defaultValue = "false") boolean fresh,
        Model model,
        Authentication authentication
    ) {
        var activeDraft = drafts.findLatestActive(
            authentication.getName(),
            FormDraftType.OBLIGATION
        );

        if (draftKey != null && !draftKey.isBlank()) {
            if (activeDraft.isEmpty()
                || !activeDraft.orElseThrow().contextKey().equals(draftKey.trim())) {
                return "redirect:/obligations/new";
            }
            ObligationForm form = model.containsAttribute("obligationForm")
                ? (ObligationForm) model.getAttribute("obligationForm")
                : new ObligationForm();
            form.setDraftKey(activeDraft.orElseThrow().contextKey());
            populateForm(model, form, "auto");
            return "obligations/form";
        }

        if (activeDraft.isPresent()) {
            model.addAttribute("obligationDraft", toCard(activeDraft.orElseThrow()));
            return "obligations/draft-decision";
        }

        ObligationForm form = model.containsAttribute("obligationForm")
            ? (ObligationForm) model.getAttribute("obligationForm")
            : new ObligationForm();
        if (form.getDraftKey() == null || form.getDraftKey().isBlank()) {
            form.setDraftKey("draft:" + UUID.randomUUID());
        }
        populateForm(model, form, "none");
        return "obligations/form";
    }

    @PostMapping("/draft/discard")
    public String discardDraft(
        @RequestParam(defaultValue = "list") String next,
        Authentication authentication,
        RedirectAttributes redirect
    ) {
        drafts.findLatestActive(authentication.getName(), FormDraftType.OBLIGATION)
            .ifPresent(draft -> drafts.discard(
                authentication.getName(),
                FormDraftType.OBLIGATION,
                draft.contextKey()
            ));
        redirect.addFlashAttribute("successMessage", "Rascunho de obrigação descartado.");
        return "new".equals(next)
            ? "redirect:/obligations/new?fresh=true"
            : "redirect:/obligations";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("obligationForm") ObligationForm form,
        BindingResult result,
        Model model,
        RedirectAttributes redirect,
        Authentication authentication
    ) {
        if (!result.hasErrors()) {
            try {
                var obligation = submissions.submit(authentication.getName(), form);
                redirect.addFlashAttribute("successMessage", "Obrigação cadastrada.");
                return "redirect:/obligations/" + obligation.getId();
            } catch (IllegalArgumentException exception) {
                result.reject("obligation", exception.getMessage());
            }
        }
        populateForm(model, form, "none");
        return "obligations/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("obligation", service.get(id));
        model.addAttribute("schedule", service.schedule(id));
        model.addAttribute("payments", service.paymentHistory(id));
        return "obligations/detail";
    }

    @GetMapping("/{id}/payments/new")
    public String paymentForm(@PathVariable UUID id, Model model) {
        model.addAttribute("obligation", service.get(id));
        model.addAttribute("paymentForm", new PaymentForm());
        return "obligations/payment-form";
    }

    @PostMapping("/{id}/payments")
    public String pay(
        @PathVariable UUID id,
        @Valid @ModelAttribute PaymentForm form,
        BindingResult result,
        Model model,
        RedirectAttributes redirect
    ) {
        if (result.hasErrors()) {
            model.addAttribute("obligation", service.get(id));
            return "obligations/payment-form";
        }
        service.pay(
            id,
            form.getDate(),
            form.getPrincipal(),
            form.getInterest(),
            form.getExtra(),
            form.getExternalReference(),
            form.getNotes()
        );
        redirect.addFlashAttribute("successMessage", "Pagamento registrado.");
        return "redirect:/obligations/" + id;
    }

    private void populateForm(Model model, ObligationForm form, String recoveryMode) {
        model.addAttribute("obligationForm", form);
        model.addAttribute("vehicles", vehicles.listAll());
        model.addAttribute("draftRecoveryMode", recoveryMode);
    }

    private ObligationDraftCard toCard(FormDraftService.DraftView view) {
        String creditor = view.payload().path("creditor").asText("").trim();
        if (creditor.isBlank()) {
            creditor = "Obrigação sem credor";
        }
        return new ObligationDraftCard(
            view.contextKey(),
            creditor,
            view.currentStep(),
            view.updatedAt()
        );
    }

    public record ObligationDraftCard(
        String contextKey,
        String creditor,
        int currentStep,
        LocalDateTime updatedAt
    ) {
    }
}
