package dev.harrison.rendacomcarro.expense.application;

import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.expense.domain.MonthlyOdometerClosing;
import dev.harrison.rendacomcarro.expense.web.MonthlyOdometerClosingForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonthlyClosingFormSubmissionService {
    private final MonthlyOdometerClosingService closings;
    private final FormDraftService drafts;

    public MonthlyClosingFormSubmissionService(
        MonthlyOdometerClosingService closings,
        FormDraftService drafts
    ) {
        this.closings = closings;
        this.drafts = drafts;
    }

    @Transactional
    public MonthlyOdometerClosing submit(
        String username,
        MonthlyOdometerClosingForm form
    ) {
        MonthlyOdometerClosing closing = closings.confirm(
            new MonthlyOdometerClosingService.ConfirmCommand(
                form.getVehicleId(),
                form.getMonth(),
                form.isManualAdjustment(),
                form.getInitialOdometer(),
                form.getFinalOdometer(),
                form.getProfessionalKilometers(),
                form.getAdjustmentReason(),
                form.isConfirmWarnings()
            )
        );
        drafts.complete(
            username,
            FormDraftType.MILEAGE_CLOSING,
            form.draftContextKey()
        );
        return closing;
    }
}
