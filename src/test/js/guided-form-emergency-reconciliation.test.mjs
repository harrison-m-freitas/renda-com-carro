import test from "node:test";
import assert from "node:assert/strict";

import { GuidedFormController } from "../../main/resources/static/js/guided-form.js";

test("an emergency copy with a deleted server draft is recreated without its stale version", async () => {
  const originalDocument = globalThis.document;
  const originalCustomEvent = globalThis.CustomEvent;
  const originalNavigator = globalThis.navigator;
  globalThis.document = { dispatchEvent() {} };
  globalThis.CustomEvent = class CustomEvent {
    constructor(type, options = {}) {
      this.type = type;
      this.detail = options.detail;
    }
  };
  Object.defineProperty(globalThis, "navigator", {
    configurable: true,
    value: { onLine: true },
  });

  const contextKey = "current";
  const saveCalls = [];
  const client = {
    readEmergency() {
      return {
        state: {
          contextKey,
          schemaVersion: 1,
          currentStep: 2,
          version: 7,
          validateCurrentStep: false,
          force: false,
          payload: { notes: "Gasto recuperado" },
        },
        savedAt: "2026-07-14T10:00:00Z",
      };
    },
    async load() {
      return null;
    },
    async save(type, state) {
      saveCalls.push({ type, state: structuredClone(state) });
      return {
        contextKey,
        version: 0,
        updatedAt: "2026-07-14T10:01:00Z",
      };
    },
  };
  const form = createForm(contextKey);

  try {
    const controller = new GuidedFormController(form, { client });
    await controller.reconcileEmergencyCopy();

    assert.equal(saveCalls.length, 1);
    assert.equal(saveCalls[0].type, "EXPENSE");
    assert.equal(
      saveCalls[0].state.version,
      null,
      "a missing server draft must be recreated instead of updating a deleted version",
    );
    assert.equal(saveCalls[0].state.payload.notes, "Gasto recuperado");
  } finally {
    restoreGlobal("document", originalDocument);
    restoreGlobal("CustomEvent", originalCustomEvent);
    restoreGlobal("navigator", originalNavigator);
  }
});

function createForm(contextKey) {
  const fields = [
    {
      name: "notes",
      value: "",
      type: "text",
      disabled: false,
      readOnly: false,
      dataset: {},
    },
  ];
  return {
    dataset: {
      draftType: "EXPENSE",
      draftSchemaVersion: "1",
      draftCurrentStep: "1",
      draftContextKey: contextKey,
      draftVersion: "",
      formMaxStep: "4",
    },
    elements: fields,
    querySelectorAll() { return []; },
  };
}

function restoreGlobal(name, originalValue) {
  if (originalValue === undefined) delete globalThis[name];
  else {
    Object.defineProperty(globalThis, name, {
      configurable: true,
      writable: true,
      value: originalValue,
    });
  }
}
