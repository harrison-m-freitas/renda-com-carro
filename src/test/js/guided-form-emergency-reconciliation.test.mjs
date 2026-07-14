import test from "node:test";
import assert from "node:assert/strict";

import { FormDraftClient } from "../../main/resources/static/js/form-draft-client.js";

test("a stale emergency version is retried as creation only after the server confirms the draft is missing", async () => {
  const requests = [];
  let putCount = 0;
  const storage = createStorage();
  const fetchImpl = async (url, options = {}) => {
    const method = options.method ?? "GET";
    requests.push({ url, method, body: options.body ?? null });

    if (method === "GET") {
      return new Response(null, { status: 404 });
    }

    putCount += 1;
    if (putCount === 1) {
      return jsonResponse({
        message: "O rascunho informado não existe mais.",
      }, 409);
    }

    return jsonResponse({
      contextKey: "current",
      version: 0,
      updatedAt: "2026-07-14T10:01:00Z",
    });
  };
  const client = new FormDraftClient({
    fetchImpl,
    storage,
    csrfToken: "",
    csrfHeader: "",
  });

  const saved = await client.save("EXPENSE", {
    contextKey: "current",
    schemaVersion: 1,
    currentStep: 2,
    version: 7,
    validateCurrentStep: false,
    force: false,
    payload: { notes: "Gasto recuperado" },
  });

  assert.equal(saved.version, 0);
  assert.deepEqual(
    requests.map((request) => request.method),
    ["PUT", "GET", "PUT"],
  );
  assert.equal(JSON.parse(requests[0].body).version, 7);
  assert.equal(
    JSON.parse(requests[2].body).version,
    null,
    "the retry must create a new draft instead of updating the deleted version",
  );
  assert.equal(storage.size, 0, "the emergency copy is cleared after the successful retry");
});

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function createStorage() {
  const values = new Map();
  return {
    get size() { return values.size; },
    getItem(key) { return values.get(key) ?? null; },
    setItem(key, value) { values.set(key, String(value)); },
    removeItem(key) { values.delete(key); },
  };
}
