package dev.harrison.rendacomcarro.expense.application;

import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.expense.domain.Expense;
import dev.harrison.rendacomcarro.expense.web.ExpenseForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseFormSubmissionService {
    private final ExpenseService expenses;
    private final FormDraftService drafts;

    public ExpenseFormSubmissionService(
        ExpenseService expenses,
        FormDraftService drafts
    ) {
        this.expenses = expenses;
        this.drafts = drafts;
    }

    @Transactional
    public Expense submit(String username, ExpenseForm form) {
        Expense created = expenses.create(new ExpenseService.CreateExpenseCommand(
            form.getVehicleId(),
            form.getOperationalDayId(),
            form.getShiftId(),
            form.getCategoryId(),
            form.getExpenseDate(),
            form.getCompetenceMonth().atDay(1),
            form.getPaidDate(),
            form.getAmount(),
            form.getClassification(),
            form.getAllocationMethod(),
            form.professionalPercentageRatio(),
            form.getProfessionalFixedAmount(),
            form.getAdjustmentReason(),
            form.getNotes()
        ));
        drafts.complete(username, FormDraftType.EXPENSE, "current");
        return created;
    }
}
