import test from "node:test";
import assert from "node:assert/strict";
import {
  calculateGoalRates,
  calculateOperationalSuggestion,
  buildOperationalSuggestionUrl,
} from "../../main/resources/static/js/goal-financial-planner.js";

test("goal rates calculate per-day and per-hour values from minutes", () => {
  const rates = calculateGoalRates({
    operationalGoal: 4000,
    personalGoal: 2100,
    dayCount: 9,
    totalMinutes: 4590,
  });
  assert.equal(rates.operationalPerDay.toFixed(2), "444.44");
  assert.equal(rates.operationalPerHour.toFixed(2), "52.29");
  assert.equal(rates.personalPerDay.toFixed(2), "233.33");
  assert.equal(rates.personalPerHour.toFixed(2), "27.45");
});

test("goal rates remain unavailable until days and workload are known", () => {
  const rates = calculateGoalRates({ operationalGoal: 4000, personalGoal: 2100, dayCount: 0, totalMinutes: 0 });
  assert.equal(rates.operationalPerDay, null);
  assert.equal(rates.operationalPerHour, null);
});

test("operational suggestion adds personal goal after professional costs", () => {
  assert.equal(calculateOperationalSuggestion(2100, 2730), 4830);
});

test("suggestion URL repeats selected vehicle ids without losing the month", () => {
  assert.equal(
    buildOperationalSuggestionUrl("2026-07", ["b", "a"]),
    "/goals/operational-suggestion?month=2026-07&vehicleIds=b&vehicleIds=a",
  );
});
