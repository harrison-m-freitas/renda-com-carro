import test from "node:test";
import assert from "node:assert/strict";
import {
  buildGoalCalendarDays,
  datesForGoalWeekdays,
  formatGoalCalendarDate,
  serializePlannedDates,
} from "../../main/resources/static/js/goal-calendar-picker.js";

test("goal calendar uses Monday-first layout and disables Sundays", () => {
  const days = buildGoalCalendarDays("2026-07");
  assert.equal(days[0].iso, null);
  assert.equal(days[1].iso, null);
  assert.equal(days[2].iso, "2026-07-01");
  assert.equal(days.find((day) => day.iso === "2026-07-05").disabled, true);
  assert.equal(days.find((day) => day.iso === "2026-07-06").weekday, 1);
});

test("goal calendar presets select weekdays and Monday through Saturday", () => {
  const weekdays = datesForGoalWeekdays("2026-07", [1, 2, 3, 4, 5]);
  assert.equal(weekdays.includes("2026-07-04"), false);
  assert.equal(weekdays.includes("2026-07-05"), false);
  assert.equal(weekdays.includes("2026-07-06"), true);

  const mondayThroughSaturday = datesForGoalWeekdays("2026-07", [1, 2, 3, 4, 5, 6]);
  assert.equal(mondayThroughSaturday.includes("2026-07-04"), true);
  assert.equal(mondayThroughSaturday.includes("2026-07-05"), false);
});

test("goal calendar serializes unique sorted dates and exposes full Portuguese labels", () => {
  assert.equal(
    serializePlannedDates(["2026-07-20", "2026-07-14", "2026-07-20"]),
    "2026-07-14,2026-07-20",
  );
  assert.match(formatGoalCalendarDate("2026-07-14"), /terça-feira, 14 de julho de 2026/i);
});
