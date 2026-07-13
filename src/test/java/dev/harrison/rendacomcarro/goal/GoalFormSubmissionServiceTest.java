package dev.harrison.rendacomcarro.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.application.FormDraftService.SaveDraftCommand;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.goal.application.GoalFormSubmissionService;
import dev.harrison.rendacomcarro.goal.application.GoalService;
import dev.harrison.rendacomcarro.goal.web.GoalForm;
import dev.harrison.rendacomcarro.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=goal-submission-owner",
    "APP_ADMIN_PASSWORD=goal-submission-password"
})
@Transactional
class GoalFormSubmissionServiceTest extends PostgresIntegrationTest {
    @Autowired GoalFormSubmissionService submissions;
    @Autowired GoalService goals;
    @Autowired FormDraftService drafts;
    @Autowired ObjectMapper mapper;

    @Test
    void createsGoalAndDeletesMonthDraft() {
        GoalForm form = validForm(YearMonth.of(2027, 1));
        seedDraft(form);

        submissions.submit("goal-submission-owner", form);

        assertThat(goals.find(form.getMonth())).isPresent();
        assertThat(drafts.find(
            "goal-submission-owner", FormDraftType.MONTHLY_GOAL, form.draftContextKey()
        )).isEmpty();
    }

    @Test
    void duplicateGoalPreservesDraft() {
        GoalForm form = validForm(YearMonth.of(2027, 2));
        submissions.submit("goal-submission-owner", form);
        seedDraft(form);

        assertThatThrownBy(() -> submissions.submit("goal-submission-owner", form))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("já cadastrada");
        assertThat(drafts.find(
            "goal-submission-owner", FormDraftType.MONTHLY_GOAL, form.draftContextKey()
        )).isPresent();
    }

    private GoalForm validForm(YearMonth month) {
        GoalForm form = new GoalForm();
        form.setMonth(month);
        form.setPersonalNetGoal(new BigDecimal("2500.00"));
        form.setOperationalGoal(new BigDecimal("4000.00"));
        form.setPlannedHours(new BigDecimal("160"));
        form.setPlannedDates(month.atDay(1) + "," + month.atDay(2));
        return form;
    }

    private void seedDraft(GoalForm form) {
        drafts.save("goal-submission-owner", new SaveDraftCommand(
            FormDraftType.MONTHLY_GOAL,
            form.draftContextKey(),
            1,
            2,
            null,
            mapper.createObjectNode()
                .put("month", form.getMonth().toString())
                .put("personalNetGoal", "2500,00")
                .put("operationalGoal", "4000,00")
                .put("plannedHours", "160")
                .put("plannedDates", form.getPlannedDates()),
            false
        ));
    }
}
