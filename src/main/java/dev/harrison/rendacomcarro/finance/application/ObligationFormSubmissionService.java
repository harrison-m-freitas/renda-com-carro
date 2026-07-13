package dev.harrison.rendacomcarro.finance.application;

import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.finance.domain.FinancialObligation;
import dev.harrison.rendacomcarro.finance.domain.ObligationMode;
import dev.harrison.rendacomcarro.finance.web.ObligationForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObligationFormSubmissionService {
    private final FinancialObligationService obligations;
    private final FormDraftService drafts;

    public ObligationFormSubmissionService(
        FinancialObligationService obligations,
        FormDraftService drafts
    ) {
        this.obligations = obligations;
        this.drafts = drafts;
    }

    @Transactional
    public FinancialObligation submit(String username, ObligationForm form) {
        validateModeSpecificFields(form);
        FinancialObligation obligation = obligations.create(
            new FinancialObligationService.CreateCommand(
                form.getVehicleId(),
                form.getType(),
                form.getMode(),
                form.getCreditor(),
                form.getPrincipal(),
                form.annualRateRatio(),
                form.getStartDate(),
                form.getFirstDueDate(),
                form.getTermMonths(),
                form.getPlannedInstallment(),
                form.getMonthlyTarget(),
                form.getNotes()
            )
        );
        drafts.complete(
            username,
            FormDraftType.OBLIGATION,
            form.draftContextKey()
        );
        return obligation;
    }

    private static void validateModeSpecificFields(ObligationForm form) {
        if (form.getMode() == ObligationMode.STRUCTURED) {
            if (form.getFirstDueDate() == null
                || form.getTermMonths() == null
                || form.getTermMonths() <= 0) {
                throw new IllegalArgumentException("Cronograma estruturado incompleto");
            }
        } else if (form.getMonthlyTarget() == null
            || form.getMonthlyTarget().signum() <= 0) {
            throw new IllegalArgumentException(
                "Meta mensal flexível deve ser maior que zero"
            );
        }
    }
}
