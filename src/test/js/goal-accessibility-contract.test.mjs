import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = async (name) => readFile(
  new URL(`../../main/resources/static/js/${name}`, import.meta.url),
  "utf8",
);

test("goal form focuses the server-side error summary", async () => {
  const code = await source("goal-form.js");
  assert.match(code, /data-form-error-summary/);
  assert.match(code, /\.focus\(\)/);
});

test("calendar exposes column headers and grid cells", async () => {
  const code = await source("goal-calendar-picker.js");
  assert.match(code, /setAttribute\('role', 'columnheader'\)/);
  assert.match(code, /setAttribute\('role', 'gridcell'\)/);
});
