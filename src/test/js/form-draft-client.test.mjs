import test from "node:test";
import assert from "node:assert/strict";
import {
  DraftConflictError,
  FormDraftClient,
} from "../../main/resources/static/js/form-draft-client.js";

test("save sends csrf version and force and clears emergency cache", async () => {
  const calls = [];
  const storage = fakeStorage();
  storage.setItem("renda:draft:EXPENSE:current", JSON.stringify({ pending: true }));
  const client = new FormDraftClient({
    fetchImpl: async (url, options) => {
      calls.push({ url, options });
      return jsonResponse(200, {
        contextKey: "current",
        version: 3,
        payload: { amount: "120.00" },
      });
    },
    storage,
    csrfToken: "csrf-token",
    csrfHeader: "X-CSRF-TOKEN",
  });

  const result = await client.save("EXPENSE", {
    contextKey: "current",
    schemaVersion: 1,
    currentStep: 2,
    version: 2,
    force: true,
    payload: { amount: "120,00" },
  });

  assert.equal(result.version, 3);
  assert.equal(calls[0].url, "/api/form-drafts/EXPENSE");
  assert.equal(calls[0].options.method, "PUT");
  assert.equal(calls[0].options.credentials, "same-origin");
  assert.equal(calls[0].options.headers["X-CSRF-TOKEN"], "csrf-token");
  assert.deepEqual(JSON.parse(calls[0].options.body), {
    contextKey: "current",
    schemaVersion: 1,
    currentStep: 2,
    version: 2,
    force: true,
    payload: { amount: "120,00" },
  });
  assert.equal(storage.getItem("renda:draft:EXPENSE:current"), null);
});

test("network failure preserves unsynchronized payload locally", async () => {
  const storage = fakeStorage();
  const client = new FormDraftClient({
    fetchImpl: async () => {
      throw new TypeError("network down");
    },
    storage,
    csrfToken: "token",
    csrfHeader: "X-CSRF-TOKEN",
  });
  const state = {
    contextKey: "current",
    schemaVersion: 1,
    currentStep: 1,
    version: null,
    force: false,
    payload: { amount: "90,00" },
  };

  await assert.rejects(() => client.save("EXPENSE", state), /network down/);
  const cached = JSON.parse(storage.getItem("renda:draft:EXPENSE:current"));
  assert.deepEqual(cached.state, state);
  assert.equal(typeof cached.savedAt, "string");
});

test("http conflict exposes the server version", async () => {
  const client = new FormDraftClient({
    fetchImpl: async () => jsonResponse(409, {
      message: "Este rascunho foi alterado em outro dispositivo.",
      current: { contextKey: "current", version: 4, payload: { amount: "140.00" } },
    }),
    storage: fakeStorage(),
    csrfToken: "token",
    csrfHeader: "X-CSRF-TOKEN",
  });

  await assert.rejects(
    () => client.save("EXPENSE", {
      contextKey: "current",
      schemaVersion: 1,
      currentStep: 1,
      version: 2,
      force: false,
      payload: { amount: "120,00" },
    }),
    (error) => {
      assert.ok(error instanceof DraftConflictError);
      assert.equal(error.current.version, 4);
      return true;
    },
  );
});

test("discard includes csrf and list/load use encoded urls", async () => {
  const calls = [];
  const client = new FormDraftClient({
    fetchImpl: async (url, options = {}) => {
      calls.push({ url, options });
      if (options.method === "DELETE") return emptyResponse(204);
      return jsonResponse(200, []);
    },
    storage: fakeStorage(),
    csrfToken: "token",
    csrfHeader: "X-CSRF-TOKEN",
  });

  await client.load("MONTHLY_GOAL", "month:2026-07");
  await client.list("OBLIGATION");
  await client.discard("EXPENSE", "current", 2);

  assert.equal(
    calls[0].url,
    "/api/form-drafts/MONTHLY_GOAL?contextKey=month%3A2026-07",
  );
  assert.equal(calls[1].url, "/api/form-drafts/OBLIGATION/list");
  assert.equal(
    calls[2].url,
    "/api/form-drafts/EXPENSE?contextKey=current&version=2",
  );
  assert.equal(calls[2].options.headers["X-CSRF-TOKEN"], "token");
});

function fakeStorage() {
  const values = new Map();
  return {
    getItem(key) { return values.has(key) ? values.get(key) : null; },
    setItem(key, value) { values.set(key, String(value)); },
    removeItem(key) { values.delete(key); },
  };
}

function jsonResponse(status, payload) {
  return {
    ok: status >= 200 && status < 300,
    status,
    async json() { return payload; },
    async text() { return JSON.stringify(payload); },
  };
}

function emptyResponse(status) {
  return {
    ok: status >= 200 && status < 300,
    status,
    async json() { return null; },
    async text() { return ""; },
  };
}
