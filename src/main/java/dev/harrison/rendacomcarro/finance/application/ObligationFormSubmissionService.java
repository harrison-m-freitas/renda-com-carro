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
        FinancialObligation obligation = obligations.create(
            new FinancialObligationService.CreateCommand(
                form.getVehicleId(),
                form.getAcquisitionPlanId(),
                form.getType(),
                form.getMode(),
                form.getCalculationMethod(),
                form.getCreditor(),
                form.getPrincipalAmount(),
                form.interestRateRatio(),
                form.getInterestRatePeriod(),
                form.getStartDate(),
                form.getFirstDueDate(),
                form.getTermMonths(),
                form.getInstallmentAmount(),
                form.getSinglePaymentAmount(),
                form.getMode() == ObligationMode.FLEXIBLE_PAYMENTS
                    ? form.getMonthlyTarget()
                    : null,
                form.getNotes()
            )
        );
        drafts.complete(username, FormDraftType.OBLIGATION, form.draftContextKey());
        return obligation;
    }
}
