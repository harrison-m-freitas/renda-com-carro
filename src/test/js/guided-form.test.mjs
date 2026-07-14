import test from "node:test";
import assert from "node:assert/strict";

import { GuidedFormController } from "../../main/resources/static/js/guided-form.js";

test("immediate mobile step save waits for an in-flight autosave", async () => {
  const originalDocument = globalThis.document;
  const originalCustomEvent = globalThis.CustomEvent;
  globalThis.document = { dispatchEvent() {} };
  globalThis.CustomEvent = class CustomEvent {
    constructor(type, options = {}) {
      this.type = type;
      this.detail = options.detail;
    }
  };

  let resolveFirst;
  let resolveSecond;
  const calls = [];
  const client = {
    save(type, state) {
      calls.push({ type, state: structuredClone(state) });
      return new Promise((resolve) => {
        if (calls.length === 1) resolveFirst = resolve;
        else resolveSecond = resolve;
      });
    },
  };
  const contextKey = "draft:123e4567-e89b-12d3-a456-426614174000";
  const form = createForm("OBLIGATION", contextKey, "creditor", "Banco");

  try {
    const controller = new GuidedFormController(form, { client });
    const autosave = controller.save();
    await Promise.resolve();

    const stepSave = controller.save({
      immediate: true,
      validateCurrentStep: true,
    });
    await Promise.resolve();

    assert.equal(calls.length, 1, "a second save must not start with the stale version");

    resolveFirst({
      contextKey,
      version: 0,
      updatedAt: "2026-07-14T00:00:00Z",
    });
    await autosave;
    await Promise.resolve();

    assert.equal(calls.length, 2);
    assert.equal(calls[1].state.version, 0);
    assert.equal(calls[1].state.validateCurrentStep, true);

    resolveSecond({
      contextKey,
      version: 1,
      updatedAt: "2026-07-14T00:00:01Z",
    });
    await stepSave;
  } finally {
    restoreBrowserGlobals(originalDocument, originalCustomEvent);
  }
});

test("a trailing autosave keeps edits made while a save is in flight", async () => {
  const originalDocument = globalThis.document;
  const originalCustomEvent = globalThis.CustomEvent;
  globalThis.document = { dispatchEvent() {} };
  globalThis.CustomEvent = class CustomEvent {
    constructor(type, options = {}) {
      this.type = type;
      this.detail = options.detail;
    }
  };

  let resolveFirst;
  let resolveSecond;
  const calls = [];
  const client = {
    save(type, state) {
      calls.push({ type, state: structuredClone(state) });
      return new Promise((resolve) => {
        if (calls.length === 1) resolveFirst = resolve;
        else resolveSecond = resolve;
      });
    },
  };
  const contextKey = "draft:123e4567-e89b-12d3-a456-426614174001";
  const form = createForm("EXPENSE", contextKey, "notes", "Combustível");

  try {
    const controller = new GuidedFormController(form, { client });
    const firstSave = controller.save();
    await Promise.resolve();

    form.elements[0].value = "Combustível atualizado";
    const trailingSave = controller.save();
    await Promise.resolve();

    assert.equal(calls.length, 1, "the trailing autosave must wait for the active request");

    resolveFirst({
      contextKey,
      version: 0,
      updatedAt: "2026-07-14T00:00:00Z",
    });
    await firstSave;
    await Promise.resolve();

    assert.equal(calls.length, 2, "the latest edits must be saved after the active request");
    assert.equal(calls[1].state.version, 0);
    assert.equal(calls[1].state.payload.notes, "Combustível atualizado");

    resolveSecond({
      contextKey,
      version: 1,
      updatedAt: "2026-07-14T00:00:01Z",
    });
    await trailingSave;
  } finally {
    restoreBrowserGlobals(originalDocument, originalCustomEvent);
  }
});

function createForm(type, contextKey, fieldName, fieldValue) {
  return {
    dataset: {
      draftType: type,
      draftSchemaVersion: "1",
      draftCurrentStep: "2",
      draftContextKey: contextKey,
      draftVersion: "",
      formMaxStep: "4",
    },
    elements: [
      {
        name: fieldName,
        value: fieldValue,
        type: "text",
        disabled: false,
        readOnly: false,
        dataset: {},
      },
    ],
    querySelectorAll() { return []; },
  };
}

function restoreBrowserGlobals(originalDocument, originalCustomEvent) {
  if (originalDocument === undefined) delete globalThis.document;
  else globalThis.document = originalDocument;
  if (originalCustomEvent === undefined) delete globalThis.CustomEvent;
  else globalThis.CustomEvent = originalCustomEvent;
}
