package dev.harrison.rendacomcarro.goal.application;

import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.goal.domain.MonthlyGoal;
import dev.harrison.rendacomcarro.goal.web.GoalForm;
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
            form.getPlannedHours(),
            form.parsedPlannedDates()
        );
        drafts.complete(username, FormDraftType.MONTHLY_GOAL, form.draftContextKey());
        return goal;
    }
}
