import test from "node:test";
import assert from "node:assert/strict";
import {
  clampStep,
  draftStorageKey,
  isMobileWizard,
  nextStep,
  previousStep,
  serializeEditableFields,
} from "../../main/resources/static/js/guided-form-state.js";

test("clamps and moves wizard steps", () => {
  assert.equal(nextStep(1, 4), 2);
  assert.equal(nextStep(4, 4), 4);
  assert.equal(previousStep(1), 1);
  assert.equal(previousStep(3), 2);
  assert.equal(clampStep(8, 4), 4);
  assert.equal(clampStep(0, 4), 1);
});

test("uses the approved mobile breakpoint and storage key", () => {
  assert.equal(isMobileWizard(767), true);
  assert.equal(isMobileWizard(768), false);
  assert.equal(
    draftStorageKey("EXPENSE", "current"),
    "renda:draft:EXPENSE:current",
  );
});

test("serializes only editable safe fields", () => {
  const fields = [
    fakeField({ name: "amount", value: "120,50" }),
    fakeField({ name: "notes", value: "texto" }),
    fakeField({ name: "ignored", value: "x", draftIgnore: true }),
    fakeField({ name: "calculated", value: "99", readOnly: true }),
    fakeField({
      name: "includedCalculated",
      value: "88",
      readOnly: true,
      draftInclude: true,
    }),
    fakeField({ name: "unchecked", value: "true", type: "checkbox", checked: false }),
    fakeField({ name: "checked", value: "true", type: "checkbox", checked: true }),
    fakeField({ name: "attachment", value: "file", type: "file" }),
    fakeField({ name: "save", value: "Salvar", type: "submit" }),
  ];
  const form = { elements: fields };

  assert.deepEqual(serializeEditableFields(form), {
    amount: "120,50",
    notes: "texto",
    includedCalculated: "88",
    checked: "true",
  });
});

function fakeField({
  name,
  value,
  type = "text",
  checked = false,
  disabled = false,
  readOnly = false,
  draftIgnore = false,
  draftInclude = false,
}) {
  return {
    name,
    value,
    type,
    checked,
    disabled,
    readOnly,
    dataset: {
      ...(draftIgnore ? { draftIgnore: "" } : {}),
      ...(draftInclude ? { draftInclude: "" } : {}),
    },
  };
}
