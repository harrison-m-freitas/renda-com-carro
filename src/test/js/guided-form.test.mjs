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
  const form = {
    dataset: {
      draftType: "OBLIGATION",
      draftSchemaVersion: "1",
      draftCurrentStep: "2",
      draftContextKey: contextKey,
      draftVersion: "",
      formMaxStep: "4",
    },
    elements: [
      {
        name: "creditor",
        value: "Banco",
        type: "text",
        disabled: false,
        readOnly: false,
        dataset: {},
      },
    ],
    querySelectorAll() { return []; },
  };

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
    if (originalDocument === undefined) delete globalThis.document;
    else globalThis.document = originalDocument;
    if (originalCustomEvent === undefined) delete globalThis.CustomEvent;
    else globalThis.CustomEvent = originalCustomEvent;
  }
});
