package dev.harrison.rendacomcarro.goal.application;

import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.goal.domain.MonthlyGoal;
import dev.harrison.rendacomcarro.goal.web.GoalForm;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalFormSubmissionService {
    private final GoalService goals;
    private final FormDraftService drafts;

    public GoalFormSubmissionService(GoalService goals, FormDraftService drafts) {
        this.goals = goals;
        this.drafts = drafts;
    }

    @Transactional
    public MonthlyGoal submit(String username, GoalForm form) {
        MonthlyGoal goal = goals.create(
            form.getMonth(),
            form.getPersonalNetGoal(),
            form.getOperationalGoal(),
            form.getWorkloadPeriodicity(),
            form.enteredDurationMinutes(),
            form.parsedPlannedDates()
        );
        drafts.complete(username, FormDraftType.MONTHLY_GOAL, form.draftContextKey());
        return goal;
    }

    @Transactional
    public MonthlyGoal update(String username, UUID id, GoalForm form) {
        String originalContextKey = "month:" + goals.get(id).getMonth();
        MonthlyGoal goal = goals.update(
            id,
            form.getMonth(),
            form.getPersonalNetGoal(),
            form.getOperationalGoal(),
            form.getWorkloadPeriodicity(),
            form.enteredDurationMinutes(),
            form.parsedPlannedDates()
        );
        drafts.complete(username, FormDraftType.MONTHLY_GOAL, originalContextKey);
        if (!originalContextKey.equals(form.draftContextKey())) {
            drafts.complete(username, FormDraftType.MONTHLY_GOAL, form.draftContextKey());
        }
        return goal;
    }
}
