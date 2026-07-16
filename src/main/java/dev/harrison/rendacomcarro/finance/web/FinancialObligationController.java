package dev.harrison.rendacomcarro.finance.web;

import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.finance.application.AcquisitionPlanService;
import dev.harrison.rendacomcarro.finance.application.FinancialObligationService;
import dev.harrison.rendacomcarro.finance.application.ObligationCalculationException;
import dev.harrison.rendacomcarro.finance.application.ObligationFormSubmissionService;
import dev.harrison.rendacomcarro.finance.application.ObligationPaymentValidationException;
import dev.harrison.rendacomcarro.finance.domain.InterestRatePeriod;
import dev.harrison.rendacomcarro.finance.domain.ObligationCalculationMethod;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.domain.ObligationType;
import dev.harrison.rendacomcarro.shared.web.BrazilianBigDecimalEditor;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.core.Authentication;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/obligations")
public class FinancialObligationController {
    private final FinancialObligationService service;
    private final ObligationFormSubmissionService submissions;
    private final FormDraftService drafts;
    private final VehicleService vehicles;
    private final AcquisitionPlanService acquisitionPlans;
    private final ObligationFormValidator validator;

    public FinancialObligationController(
        FinancialObligationService service,
        ObligationFormSubmissionService submissions,
        FormDraftService drafts,
        VehicleService vehicles,
        AcquisitionPlanService acquisitionPlans,
        ObligationFormValidator validator
    ) {
        this.service = service;
        this.submissions = submissions;
        this.drafts = drafts;
        this.vehicles = vehicles;
        this.acquisitionPlans = acquisitionPlans;
        this.validator = validator;
    }

    @InitBinder("obligationForm")
    void bindObligationForm(WebDataBinder binder) {
        for (String field : new String[] {
            "principalAmount", "interestRatePercent", "installmentAmount",
            "singlePaymentAmount", "monthlyTarget"
        }) {
            binder.registerCustomEditor(BigDecimal.class, field, new BrazilianBigDecimalEditor());
        }
        binder.addValidators(validator);
    }

    @ModelAttribute("types")
    ObligationType[] types() { return ObligationType.values(); }

    @ModelAttribute("modes")
    ObligationMode[] modes() { return ObligationMode.values(); }

    @ModelAttribute("calculationMethods")
    ObligationCalculationMethod[] calculationMethods() {
        return ObligationCalculationMethod.values();
    }

    @ModelAttribute("ratePeriods")
    InterestRatePeriod[] ratePeriods() { return InterestRatePeriod.values(); }

    @GetMapping
    public String list(Model model, Authentication authentication) {
        model.addAttribute("obligations", service.list());
        model.addAttribute("acquisitionPlans", acquisitionPlans.list());
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
        @RequestParam(required = false) UUID acquisitionPlanId,
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
                return redirectToNew(acquisitionPlanId);
            }
            ObligationForm form = model.containsAttribute("obligationForm")
                ? (ObligationForm) model.getAttribute("obligationForm")
                : new ObligationForm();
            var draft = activeDraft.orElseThrow();
            form.setDraftKey(draft.contextKey());
            if (form.getAcquisitionPlanId() == null) {
                form.setAcquisitionPlanId(draftPlanId(draft));
            }
            populateForm(model, form, "auto");
            return "obligations/form";
        }

        if (activeDraft.isPresent()) {
            model.addAttribute("obligationDraft", toCard(activeDraft.orElseThrow()));
            model.addAttribute("requestedAcquisitionPlanId", acquisitionPlanId);
            return "obligations/draft-decision";
        }

        ObligationForm form = model.containsAttribute("obligationForm")
            ? (ObligationForm) model.getAttribute("obligationForm")
            : new ObligationForm();
        if (form.getDraftKey() == null || form.getDraftKey().isBlank()) {
            form.setDraftKey("draft:" + UUID.randomUUID());
        }
        if (form.getAcquisitionPlanId() == null) {
            form.setAcquisitionPlanId(acquisitionPlanId);
        }
        populateForm(model, form, "none");
        return "obligations/form";
    }

    @PostMapping("/draft/discard")
    public String discardDraft(
        @RequestParam(defaultValue = "list") String next,
        @RequestParam(required = false) UUID acquisitionPlanId,
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
        if (!"new".equals(next)) {
            return "redirect:/obligations";
        }
        return acquisitionPlanId == null
            ? "redirect:/obligations/new?fresh=true"
            : "redirect:/obligations/new?fresh=true&acquisitionPlanId=" + acquisitionPlanId;
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
                if (form.getAcquisitionPlanId() != null) {
                    return "redirect:/acquisition-plans/" + form.getAcquisitionPlanId();
                }
                return "redirect:/obligations/" + obligation.getId();
            } catch (ObligationCalculationException exception) {
                result.rejectValue(exception.field(), "calculation", exception.getMessage());
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
        try {
            service.pay(
                id, form.getDate(), form.getPrincipal(), form.getInterest(), form.getExtra(),
                form.getExternalReference(), form.getNotes()
            );
        } catch (ObligationPaymentValidationException exception) {
            result.rejectValue(exception.field(), "payment", exception.getMessage());
            model.addAttribute("obligation", service.get(id));
            return "obligations/payment-form";
        }
        redirect.addFlashAttribute("successMessage", "Pagamento registrado.");
        return "redirect:/obligations/" + id;
    }

    private void populateForm(Model model, ObligationForm form, String recoveryMode) {
        if (form.getAcquisitionPlanId() != null) {
            var plan = acquisitionPlans.get(form.getAcquisitionPlanId());
            if (plan.getVehicle() != null) {
                form.setVehicleId(plan.getVehicle().getId());
            }
            model.addAttribute(
                "acquisitionPlanSummary",
                acquisitionPlans.summary(form.getAcquisitionPlanId())
            );
        }
        model.addAttribute("obligationForm", form);
        model.addAttribute("vehicles", vehicles.listAll());
        model.addAttribute("draftRecoveryMode", recoveryMode);
    }

    private String redirectToNew(UUID acquisitionPlanId) {
        return acquisitionPlanId == null
            ? "redirect:/obligations/new"
            : "redirect:/obligations/new?acquisitionPlanId=" + acquisitionPlanId;
    }

    private UUID draftPlanId(FormDraftService.DraftView draft) {
        String raw = draft.payload().path("acquisitionPlanId").asText("").trim();
        return raw.isEmpty() ? null : UUID.fromString(raw);
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
    ) {}
}
