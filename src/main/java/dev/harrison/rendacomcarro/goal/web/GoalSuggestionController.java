package dev.harrison.rendacomcarro.goal.web;

import dev.harrison.rendacomcarro.goal.application.GoalService;
import dev.harrison.rendacomcarro.goal.application.OperationalGoalSuggestionService;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import java.time.YearMonth;
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
    private final GoalService goals;
    private final VehicleService vehicles;

    public GoalSuggestionController(
        OperationalGoalSuggestionService suggestions,
        GoalService goals,
        VehicleService vehicles
    ) {
        this.suggestions = suggestions;
        this.goals = goals;
        this.vehicles = vehicles;
    }

    @GetMapping("/operational-suggestion")
    public OperationalGoalSuggestionResponse suggestion(
        @RequestParam
        @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
        @RequestParam(required = false) UUID goalId
    ) {
        UUID vehicleId = goalId == null
            ? vehicles.findActiveVehicle()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Cadastre ou ative um veículo antes de calcular a sugestão."
                )).getId()
            : goals.get(goalId).getVehicle().getId();
        return OperationalGoalSuggestionResponse.from(
            suggestions.suggest(month, vehicleId)
        );
    }
}
