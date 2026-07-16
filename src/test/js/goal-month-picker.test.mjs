import test from "node:test";
import assert from "node:assert/strict";
import {
  formatGoalMonth,
  goalMonthValue,
  monthLabelsForYear,
  prepareGoalMonthInput,
} from "../../main/resources/static/js/goal-month-picker.js";

test("goal month picker formats the Portuguese month with only the first letter capitalized", () => {
  assert.equal(formatGoalMonth("2026-07"), "Julho de 2026");
  assert.equal(formatGoalMonth("2026-01"), "Janeiro de 2026");
  assert.equal(formatGoalMonth("invalid"), "");
});

test("goal month picker builds a stable twelve-month year grid", () => {
  assert.equal(goalMonthValue(2026, 0), "2026-01");
  assert.equal(goalMonthValue(2026, 11), "2026-12");
  const options = monthLabelsForYear(2026);
  assert.equal(options.length, 12);
  assert.deepEqual(options.at(6), { value: "2026-07", label: "Julho" });
});


test("enhancing the month field disables hidden native validation and restores it on destroy", () => {
  const input = { hidden: false, required: true };

  const restore = prepareGoalMonthInput(input);

  assert.equal(input.hidden, true);
  assert.equal(input.required, false);

  restore();
  assert.equal(input.hidden, false);
  assert.equal(input.required, true);
});
