import test from "node:test";
import assert from "node:assert/strict";

import { GuidedFormController } from "../../main/resources/static/js/guided-form.js";

const GUIDED_DRAFT_TYPES = [
  "EXPENSE",
  "MILEAGE_CLOSING",
  "MONTHLY_GOAL",
  "OBLIGATION",
];

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

test("a queued save still runs with the latest payload after the active save fails", async () => {
  const originalDocument = globalThis.document;
  const originalCustomEvent = globalThis.CustomEvent;
  globalThis.document = { dispatchEvent() {} };
  globalThis.CustomEvent = class CustomEvent {
    constructor(type, options = {}) {
      this.type = type;
      this.detail = options.detail;
    }
  };

  let rejectFirst;
  let resolveSecond;
  const calls = [];
  const client = {
    save(type, state) {
      calls.push({ type, state: structuredClone(state) });
      return new Promise((resolve, reject) => {
        if (calls.length === 1) rejectFirst = reject;
        else resolveSecond = resolve;
      });
    },
  };
  const contextKey = "draft:obligation-failure";
  const form = createForm("OBLIGATION", contextKey, "creditor", "Banco");

  try {
    const controller = new GuidedFormController(form, { client });
    const firstSave = controller.save();
    const firstFailure = assert.rejects(firstSave, /network failure/);
    await Promise.resolve();

    form.elements[0].value = "Banco atualizado";
    const trailingSave = controller.save();
    const trailingOutcome = trailingSave.then(
      (value) => ({ value }),
      (error) => ({ error }),
    );
    await Promise.resolve();

    assert.equal(calls.length, 1, "the trailing save must wait for the active request");

    rejectFirst(new Error("network failure"));
    await firstFailure;
    await Promise.resolve();

    assert.equal(calls.length, 2, "the queued save must retry after the failed request settles");
    assert.equal(calls[1].state.version, null);
    assert.equal(calls[1].state.payload.creditor, "Banco atualizado");

    resolveSecond({
      contextKey,
      version: 0,
      updatedAt: "2026-07-14T00:00:01Z",
    });
    const outcome = await trailingOutcome;
    assert.equal(outcome.error, undefined);
    assert.equal(outcome.value.version, 0);
  } finally {
    restoreBrowserGlobals(originalDocument, originalCustomEvent);
  }
});

for (const draftType of GUIDED_DRAFT_TYPES) {
  test(`${draftType} keeps a trailing autosave made while a save is in flight`, async () => {
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
    const contextKey = `draft:${draftType.toLowerCase()}`;
    const form = createForm(draftType, contextKey, "notes", "Valor inicial");

    try {
      const controller = new GuidedFormController(form, { client });
      const firstSave = controller.save();
      await Promise.resolve();

      form.elements[0].value = "Valor atualizado";
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
      assert.equal(calls[1].type, draftType);
      assert.equal(calls[1].state.version, 0);
      assert.equal(calls[1].state.payload.notes, "Valor atualizado");

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
}

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
