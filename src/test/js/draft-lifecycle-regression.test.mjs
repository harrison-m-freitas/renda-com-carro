import test from "node:test";
import assert from "node:assert/strict";

import { GuidedFormController } from "../../main/resources/static/js/guided-form.js";
import { draftPayloadsEqual } from "../../main/resources/static/js/guided-form-state.js";
import { bootstrapExpenseDraftForm } from "../../main/resources/static/js/expense-draft-policy.js";

test("equivalent localized and server payloads do not create a material conflict", () => {
  assert.equal(
    draftPayloadsEqual(
      { amount: "100,00", classification: "PROFESSIONAL" },
      { classification: "PROFESSIONAL", amount: "100.00" },
    ),
    true,
  );
});

test("a clean form does not save during pagehide", async () => {
  const client = createClient();
  const controller = new GuidedFormController(createGuidedForm(), { client });
  controller.initialPayload = { notes: "" };
  controller.dirty = false;

  await controller.handlePagehide();

  assert.equal(client.saveCalls.length, 0);
});

test("discarded draft cannot be recreated by pagehide", async () => {
  const client = createClient();
  const controller = new GuidedFormController(createGuidedForm(), { client });
  controller.dirty = true;

  await controller.discardCurrentDraft();
  await controller.handlePagehide();

  assert.deepEqual(client.discardCalls, [["EXPENSE", "expense:new:123e4567-e89b-12d3-a456-426614174000"]]);
  assert.equal(client.saveCalls.length, 0);
  assert.equal(controller.disposed, true);
});

test("expense form starts with a unique session key and disables modal recovery", () => {
  const storage = createStorage();
  const inputs = [];
  const form = {
    dataset: { draftContextKey: "current" },
    ownerDocument: {
      createElement() {
        return { dataset: {}, value: "", type: "", name: "" };
      },
    },
    querySelector(selector) {
      if (selector === "[data-expense-error-summary]:not([hidden])") return null;
      const match = /^input\[name="([^"]+)"\]$/.exec(selector);
      return match ? inputs.find((input) => input.name === match[1]) ?? null : null;
    },
    prepend(input) { inputs.unshift(input); },
    addEventListener() {},
  };
  const documentObject = {
    readyState: "loading",
    querySelector(selector) { return selector === "#expense-form" ? form : null; },
    addEventListener() {},
  };
  const windowObject = {
    sessionStorage: storage,
    crypto: { randomUUID: () => "123e4567-e89b-12d3-a456-426614174000" },
  };

  const result = bootstrapExpenseDraftForm(documentObject, windowObject);

  assert.equal(result.currentKey, "expense:new:123e4567-e89b-12d3-a456-426614174000");
  assert.equal(form.dataset.draftRecoveryMode, "none");
  assert.equal(inputs.find((input) => input.name === "draftContextKey").value, result.currentKey);
  assert.equal(inputs.find((input) => input.name === "previousDraftContextKey").value, "");
});

function createGuidedForm() {
  return {
    dataset: {
      draftType: "EXPENSE",
      draftSchemaVersion: "2",
      draftCurrentStep: "1",
      draftContextKey: "expense:new:123e4567-e89b-12d3-a456-426614174000",
      draftVersion: "",
      formMaxStep: "3",
    },
    elements: [],
    querySelectorAll() { return []; },
    querySelector() { return null; },
  };
}

function createClient() {
  return {
    saveCalls: [],
    discardCalls: [],
    async save(type, state) {
      this.saveCalls.push([type, structuredClone(state)]);
      return { contextKey: state.contextKey, version: 0, updatedAt: "2026-07-15T00:00:00Z" };
    },
    async discard(type, contextKey) {
      this.discardCalls.push([type, contextKey]);
    },
    clearEmergency() {},
  };
}

function createStorage() {
  const values = new Map();
  return {
    get length() { return values.size; },
    getItem(key) { return values.has(key) ? values.get(key) : null; },
    setItem(key, value) { values.set(key, String(value)); },
    removeItem(key) { values.delete(key); },
    key(index) { return Array.from(values.keys())[index] ?? null; },
  };
}
