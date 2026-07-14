package dev.harrison.rendacomcarro.goal.web;

import dev.harrison.rendacomcarro.goal.application.OperationalGoalSuggestionService;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/goals")
public class GoalSuggestionController {
    private final OperationalGoalSuggestionService suggestions;

    public GoalSuggestionController(OperationalGoalSuggestionService suggestions) {
        this.suggestions = suggestions;
    }

    @GetMapping("/operational-suggestion")
    public OperationalGoalSuggestionResponse suggestion(
        @RequestParam
        @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
        @RequestParam(required = false) Set<UUID> vehicleIds
    ) {
        return OperationalGoalSuggestionResponse.from(
            suggestions.suggest(month, vehicleIds)
        );
    }
}
