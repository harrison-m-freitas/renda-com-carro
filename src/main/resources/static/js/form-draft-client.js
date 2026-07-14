import {
  draftStorageKey,
  isDraftFrameworkControlName,
} from "./guided-form-state.js";

export class DraftConflictError extends Error {
  constructor(message, current) {
    super(message);
    this.name = "DraftConflictError";
    this.current = current;
  }
}

export class DraftHttpError extends Error {
  constructor(message, status) {
    super(message);
    this.name = "DraftHttpError";
    this.status = status;
  }
}

export class FormDraftClient {
  constructor({
    fetchImpl = globalThis.fetch?.bind(globalThis),
    storage = globalThis.localStorage,
    csrfToken = readMeta("_csrf"),
    csrfHeader = readMeta("_csrf_header"),
    baseUrl = "/api/form-drafts",
  } = {}) {
    if (typeof fetchImpl !== "function") {
      throw new Error("Fetch indisponível para sincronizar rascunhos.");
    }
    this.fetchImpl = fetchImpl;
    this.storage = storage;
    this.csrfToken = csrfToken;
    this.csrfHeader = csrfHeader;
    this.baseUrl = baseUrl.replace(/\/$/, "");
  }

  async load(type, contextKey) {
    const url = `${this.baseUrl}/${encodeURIComponent(type)}?contextKey=${encodeURIComponent(contextKey)}`;
    const response = await this.fetchImpl(url, { credentials: "same-origin" });
    if (response.status === 404) return null;
    return this.#readJson(response);
  }

  async list(type) {
    const url = `${this.baseUrl}/${encodeURIComponent(type)}/list`;
    const response = await this.fetchImpl(url, { credentials: "same-origin" });
    return this.#readJson(response);
  }

  async save(type, state, { keepalive = false } = {}) {
    const storageKey = draftStorageKey(type, state.contextKey);
    this.#writeEmergency(storageKey, state);
    try {
      const response = await this.fetchImpl(
        `${this.baseUrl}/${encodeURIComponent(type)}`,
        {
          method: "PUT",
          credentials: "same-origin",
          keepalive,
          headers: this.#headers(true),
          body: JSON.stringify(state),
        },
      );
      const result = await this.#readJson(response);
      this.storage?.removeItem(storageKey);
      return result;
    } catch (error) {
      this.#writeEmergency(storageKey, state);
      throw error;
    }
  }

  async discard(type, contextKey, version = null) {
    const params = new URLSearchParams({ contextKey });
    if (version !== null && version !== undefined) {
      params.set("version", String(version));
    }
    const response = await this.fetchImpl(
      `${this.baseUrl}/${encodeURIComponent(type)}?${params.toString()}`,
      {
        method: "DELETE",
        credentials: "same-origin",
        headers: this.#headers(false),
      },
    );
    if (!response.ok) await this.#throwResponse(response);
    this.storage?.removeItem(draftStorageKey(type, contextKey));
  }

  readEmergency(type, contextKey) {
    const raw = this.storage?.getItem(draftStorageKey(type, contextKey));
    if (!raw) return null;
    try {
      return sanitizeEmergency(JSON.parse(raw));
    } catch {
      this.storage?.removeItem(draftStorageKey(type, contextKey));
      return null;
    }
  }

  clearEmergency(type, contextKey) {
    this.storage?.removeItem(draftStorageKey(type, contextKey));
  }

  #headers(includeJson) {
    const headers = {};
    if (includeJson) headers["Content-Type"] = "application/json";
    if (this.csrfHeader && this.csrfToken) {
      headers[this.csrfHeader] = this.csrfToken;
    }
    return headers;
  }

  #writeEmergency(storageKey, state) {
    this.storage?.setItem(storageKey, JSON.stringify({
      state,
      savedAt: new Date().toISOString(),
    }));
  }

  async #readJson(response) {
    if (!response.ok) return this.#throwResponse(response);
    if (response.status === 204) return null;
    return response.json();
  }

  async #throwResponse(response) {
    let body = null;
    try {
      body = await response.json();
    } catch {
      body = null;
    }
    const message = body?.message ?? "Não foi possível sincronizar o rascunho.";
    if (response.status === 409 && body?.current) {
      throw new DraftConflictError(message, body.current);
    }
    throw new DraftHttpError(message, response.status);
  }
}

function sanitizeEmergency(emergency) {
  const payload = emergency?.state?.payload;
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
    return emergency;
  }

  const sanitizedPayload = { ...payload };
  Object.keys(sanitizedPayload).forEach((name) => {
    if (isDraftFrameworkControlName(name)) delete sanitizedPayload[name];
  });

  return {
    ...emergency,
    state: {
      ...emergency.state,
      payload: sanitizedPayload,
    },
  };
}

function readMeta(name) {
  return globalThis.document
    ?.querySelector(`meta[name="${name}"]`)
    ?.getAttribute("content") ?? "";
}
