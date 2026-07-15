const ACTIVE_SESSION_KEY = "renda:expense:draft-session-key";
const PREVIOUS_SESSION_KEY = "renda:expense:previous-draft-key";
const SUBMITTED_SESSION_KEY = "renda:expense:submitted-draft-key";

bootstrapExpenseDraftForm();

export function bootstrapExpenseDraftForm(
  documentObject = globalThis.document,
  windowObject = globalThis.window,
) {
  const form = documentObject?.querySelector?.("#expense-form");
  if (!form) return null;

  const storage = safeSessionStorage(windowObject);
  const hasServerErrors = Boolean(
    form.querySelector("[data-expense-error-summary]:not([hidden])"),
  );
  const submittedKey = readStorage(storage, SUBMITTED_SESSION_KEY);
  let currentKey = readStorage(storage, ACTIVE_SESSION_KEY);

  if (!isExpenseDraftKey(currentKey) || (submittedKey && !hasServerErrors)) {
    currentKey = createExpenseDraftKey(windowObject);
    writeStorage(storage, ACTIVE_SESSION_KEY, currentKey);
    removeStorage(storage, PREVIOUS_SESSION_KEY);
  }
  removeStorage(storage, SUBMITTED_SESSION_KEY);

  form.dataset.draftContextKey = currentKey;
  form.dataset.draftRecoveryMode = "none";
  const currentInput = ensureHiddenInput(form, "draftContextKey");
  currentInput.value = currentKey;
  const previousInput = ensureHiddenInput(form, "previousDraftContextKey");
  previousInput.value = readStorage(storage, PREVIOUS_SESSION_KEY) ?? "";

  replaceLegacyDiscardButton(form);

  form.addEventListener("submit", (event) => {
    queueMicrotask(() => {
      if (!event.defaultPrevented) {
        writeStorage(storage, SUBMITTED_SESSION_KEY, currentInput.value);
        writeStorage(storage, ACTIVE_SESSION_KEY, currentInput.value);
        if (previousInput.value) {
          writeStorage(storage, PREVIOUS_SESSION_KEY, previousInput.value);
        }
      }
    });
  });

  whenDocumentReady(documentObject, () => {
    queueMicrotask(() => connectPreviousExpenseDraft({
      form,
      windowObject,
      storage,
      currentInput,
      previousInput,
    }));
  });

  return { form, currentKey, currentInput, previousInput };
}

async function connectPreviousExpenseDraft({
  form,
  windowObject,
  storage,
  currentInput,
  previousInput,
}) {
  const controller = form.guidedFormController;
  if (!controller) return;

  let drafts;
  try {
    drafts = await controller.client.list("EXPENSE");
  } catch {
    return;
  }
  const previous = Array.from(drafts ?? [])
    .find((draft) => draft.contextKey !== controller.contextKey());
  if (!previous) {
    previousInput.value = "";
    removeStorage(storage, PREVIOUS_SESSION_KEY);
    return;
  }

  previousInput.value = previous.contextKey;
  writeStorage(storage, PREVIOUS_SESSION_KEY, previous.contextKey);
  const banner = renderPreviousDraftBanner(form, previous);
  const continueButton = banner.querySelector("[data-expense-continue-previous]");
  const discardButton = banner.querySelector("[data-expense-discard-previous]");

  continueButton?.addEventListener("click", async () => {
    if (controller.dirty
        && windowObject.confirm
        && !windowObject.confirm("Descartar o preenchimento atual e continuar o rascunho salvo?")) {
      return;
    }
    continueButton.disabled = true;
    discardButton.disabled = true;
    const currentKey = controller.contextKey();
    try {
      clearTimeout(controller.saveTimer);
      controller.discarding = true;
      if (controller.savePromise) {
        try { await controller.savePromise; } catch { /* switching remains explicit */ }
      }
      if (currentKey && currentKey !== previous.contextKey) {
        await controller.client.discard("EXPENSE", currentKey);
      }
      const draft = await controller.client.load("EXPENSE", previous.contextKey);
      if (!draft) {
        banner.remove();
        previousInput.value = "";
        removeStorage(storage, PREVIOUS_SESSION_KEY);
        return;
      }
      controller.discarding = false;
      controller.disposed = false;
      controller.setDraftIdentity(draft.contextKey, draft.version);
      controller.restore(draft);
      currentInput.value = draft.contextKey;
      previousInput.value = "";
      writeStorage(storage, ACTIVE_SESSION_KEY, draft.contextKey);
      removeStorage(storage, PREVIOUS_SESSION_KEY);
      banner.remove();
    } catch {
      controller.discarding = false;
      continueButton.disabled = false;
      discardButton.disabled = false;
    }
  });

  discardButton?.addEventListener("click", async () => {
    if (windowObject.confirm
        && !windowObject.confirm("Descartar definitivamente o rascunho anterior?")) {
      return;
    }
    continueButton.disabled = true;
    discardButton.disabled = true;
    try {
      await controller.client.discard("EXPENSE", previous.contextKey);
      previousInput.value = "";
      removeStorage(storage, PREVIOUS_SESSION_KEY);
      banner.remove();
    } catch {
      continueButton.disabled = false;
      discardButton.disabled = false;
    }
  });
}

function renderPreviousDraftBanner(form, draft) {
  const existing = form.querySelector("[data-expense-previous-draft]");
  if (existing) return existing;

  const banner = form.ownerDocument.createElement("section");
  banner.className = "alert alert-info d-flex flex-column flex-md-row align-items-md-center justify-content-between gap-3";
  banner.dataset.expensePreviousDraft = "";
  banner.setAttribute("role", "region");
  banner.setAttribute("aria-labelledby", "expense-previous-draft-title");
  banner.innerHTML = `
    <div>
      <h2 class="h5 mb-1" id="expense-previous-draft-title">Existe um rascunho anterior</h2>
      <p class="mb-0">Salvo em <strong>${escapeHtml(formatDateTime(draft.updatedAt))}</strong>.</p>
    </div>
    <div class="d-flex flex-column flex-sm-row gap-2 flex-shrink-0">
      <button class="btn btn-primary" type="button" data-expense-continue-previous>Continuar rascunho</button>
      <button class="btn btn-outline-danger" type="button" data-expense-discard-previous>Descartar rascunho</button>
    </div>`;

  const firstContent = form.querySelector("[data-expense-error-summary], [data-form-step]");
  if (firstContent) form.insertBefore(banner, firstContent);
  else form.prepend(banner);
  return banner;
}

function replaceLegacyDiscardButton(form) {
  const legacy = form.querySelector("[data-expense-discard-draft]");
  if (!legacy) return;
  const replacement = legacy.cloneNode(true);
  replacement.removeAttribute("data-expense-discard-draft");
  replacement.setAttribute("data-guided-discard-current", "");
  replacement.dataset.confirmMessage = "Descartar o rascunho atual e começar com um formulário vazio?";
  replacement.dataset.redirect = "/expenses/new";
  legacy.replaceWith(replacement);
}

function ensureHiddenInput(form, name) {
  let input = form.querySelector(`input[name="${name}"]`);
  if (input) return input;
  input = form.ownerDocument.createElement("input");
  input.type = "hidden";
  input.name = name;
  input.dataset.draftIgnore = "";
  form.prepend(input);
  return input;
}

function createExpenseDraftKey(windowObject) {
  const uuid = windowObject?.crypto?.randomUUID?.()
    ?? `${Date.now().toString(16)}-0000-4000-8000-${Math.random().toString(16).slice(2, 14).padEnd(12, "0")}`;
  return `expense:new:${uuid}`;
}

function isExpenseDraftKey(value) {
  return value === "current"
    || /^expense:new:[0-9a-fA-F-]{36}$/.test(String(value ?? ""));
}

function safeSessionStorage(windowObject) {
  try {
    return windowObject?.sessionStorage ?? null;
  } catch {
    return null;
  }
}

function readStorage(storage, key) {
  try { return storage?.getItem(key) ?? null; } catch { return null; }
}

function writeStorage(storage, key, value) {
  try { storage?.setItem(key, value); } catch { /* optional enhancement */ }
}

function removeStorage(storage, key) {
  try { storage?.removeItem(key); } catch { /* optional enhancement */ }
}

function whenDocumentReady(documentObject, callback) {
  if (documentObject.readyState === "loading") {
    documentObject.addEventListener("DOMContentLoaded", callback, { once: true });
  } else {
    callback();
  }
}

function formatDateTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "horário desconhecido";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
