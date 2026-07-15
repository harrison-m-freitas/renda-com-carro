import { DraftConflictError, FormDraftClient } from "./form-draft-client.js";
import {
  clampStep,
  isMobileWizard,
  nextStep,
  previousStep,
  serializeEditableFields,
} from "./guided-form-state.js";

const AUTOSAVE_DELAY_MS = 1_500;

export class GuidedFormController {
  constructor(form, { client = new FormDraftClient() } = {}) {
    this.form = form;
    this.client = client;
    this.type = form.dataset.draftType;
    this.schemaVersion = Number(form.dataset.draftSchemaVersion || 1);
    this.maxStep = Math.max(
      1,
      Number(form.dataset.formMaxStep || form.querySelectorAll("[data-form-step]").length || 1),
    );
    this.currentStep = clampStep(Number(form.dataset.draftCurrentStep || 1), this.maxStep);
    this.version = parseNullableNumber(form.dataset.draftVersion);
    this.lastContextKey = this.contextKey();
    this.saveTimer = null;
    this.savePromise = null;
    this.pendingConflict = null;
    this.resizeHandler = () => this.renderSteps();
    this.onlineHandler = () => this.reconcileEmergencyCopy();
    this.pagehideHandler = () => {
      this.save({ keepalive: true, quiet: true }).catch(() => {});
    };
  }

  async connect() {
    this.form.classList.add("guided-form--enhanced");
    this.form.guidedFormController = this;
    this.bindEvents();
    this.renderSteps();
    await this.checkRecovery();
    await this.reconcileEmergencyCopy();
  }

  disconnect() {
    clearTimeout(this.saveTimer);
    globalThis.removeEventListener?.("resize", this.resizeHandler);
    globalThis.removeEventListener?.("online", this.onlineHandler);
    globalThis.removeEventListener?.("pagehide", this.pagehideHandler);
    delete this.form.guidedFormController;
  }

  bindEvents() {
    this.form.addEventListener("input", () => this.scheduleSave());
    this.form.addEventListener("change", () => this.scheduleSave());
    this.form.addEventListener("blur", (event) => {
      if (event.target?.matches?.("[data-save-on-blur]")) {
        this.save().catch(() => {});
      }
    }, true);

    this.form.querySelectorAll("[data-guided-next]").forEach((button) => {
      button.addEventListener("click", async () => {
        if (!this.validateCurrentStep()) return;
        try {
          await this.save({
            immediate: true,
            validateCurrentStep: true,
          });
          this.setStep(nextStep(this.currentStep, this.maxStep));
        } catch {
          // The visible save status or conflict dialog explains why progression stopped.
        }
      });
    });

    this.form.querySelectorAll("[data-guided-previous]").forEach((button) => {
      button.addEventListener("click", () => {
        this.setStep(previousStep(this.currentStep));
      });
    });

    this.form.querySelectorAll("[data-draft-recover]").forEach((button) => {
      button.addEventListener("click", () => {
        if (this.recoveryDraft) this.restore(this.recoveryDraft);
        this.hideDialog("recovery");
      });
    });

    this.form.querySelectorAll("[data-draft-discard]").forEach((button) => {
      button.addEventListener("click", async () => {
        if (!globalThis.confirm?.("Descartar o rascunho salvo e começar novamente?")) return;
        const key = this.contextKey();
        await this.client.discard(this.type, key, this.recoveryDraft?.version ?? null);
        this.version = null;
        this.lastContextKey = key;
        this.client.clearEmergency(this.type, key);
        this.hideDialog("recovery");
      });
    });

    this.form.querySelectorAll("[data-conflict-use-server]").forEach((button) => {
      button.addEventListener("click", () => {
        if (this.pendingConflict?.current) this.restore(this.pendingConflict.current);
        this.hideDialog("conflict");
      });
    });

    this.form.querySelectorAll("[data-conflict-review-local]").forEach((button) => {
      button.addEventListener("click", () => this.hideDialog("conflict"));
    });

    this.form.querySelectorAll("[data-conflict-force-local]").forEach((button) => {
      button.addEventListener("click", async () => {
        if (!globalThis.confirm?.("Substituir a versão mais recente do servidor por estes valores?")) return;
        this.version = this.pendingConflict?.current?.version ?? this.version;
        this.hideDialog("conflict");
        await this.save({ immediate: true, force: true });
      });
    });

    globalThis.addEventListener?.("resize", this.resizeHandler);
    globalThis.addEventListener?.("online", this.onlineHandler);
    globalThis.addEventListener?.("pagehide", this.pagehideHandler);
  }

  contextKey() {
    return this.form.dataset.draftContextKey?.trim() ?? "";
  }

  scheduleSave() {
    clearTimeout(this.saveTimer);
    this.saveTimer = setTimeout(() => {
      this.save().catch(() => {});
    }, AUTOSAVE_DELAY_MS);
  }

  async save({
    immediate = false,
    keepalive = false,
    quiet = false,
    force = false,
    validateCurrentStep = false,
  } = {}) {
    clearTimeout(this.saveTimer);
    if (!this.type || !this.contextKey()) return null;

    const runSave = () => {
      const payload = serializeEditableFields(this.form);
      const detail = {
        form: this.form,
        payload,
        currentStep: this.currentStep,
        contextKey: this.contextKey(),
      };
      document.dispatchEvent(new CustomEvent("guided-form:before-save", { detail }));

      if (!detail.contextKey) return Promise.resolve(null);
      if (detail.contextKey !== this.lastContextKey) {
        this.version = null;
        this.form.dataset.draftVersion = "";
        this.lastContextKey = detail.contextKey;
      }
      this.form.dataset.draftContextKey = detail.contextKey;

      const state = {
        contextKey: detail.contextKey,
        schemaVersion: this.schemaVersion,
        currentStep: this.currentStep,
        version: this.version,
        validateCurrentStep,
        force,
        payload: detail.payload,
      };

      if (!quiet) this.setSaveStatus("Salvando…", "saving");
      return this.client.save(this.type, state, { keepalive })
        .then((saved) => {
          this.version = saved.version;
          this.lastContextKey = saved.contextKey;
          this.form.dataset.draftVersion = String(saved.version);
          this.form.dataset.draftContextKey = saved.contextKey;
          if (!quiet) {
            this.setSaveStatus(
              `Rascunho salvo às ${formatTime(saved.updatedAt ?? new Date())}`,
              "saved",
            );
          }
          return saved;
        })
        .catch((error) => {
          if (error instanceof DraftConflictError) {
            this.pendingConflict = error;
            this.populateConflict(error.current);
            this.showDialog("conflict");
          }
          if (!quiet) this.setSaveStatus("Falha ao salvar — tentar novamente", "error");
          throw error;
        });
    };

    const previousSave = this.savePromise;
    const operation = previousSave
      ? previousSave.then(runSave, runSave)
      : runSave();
    const trackedOperation = operation.finally(() => {
      if (this.savePromise === trackedOperation) this.savePromise = null;
    });
    this.savePromise = trackedOperation;
    return trackedOperation;
  }

  validateCurrentStep() {
    const section = this.form.querySelector(`[data-form-step="${this.currentStep}"]`);
    if (!section) return true;
    const controls = Array.from(section.querySelectorAll("input, select, textarea"))
      .filter((field) => !field.disabled && field.type !== "hidden");
    const invalid = controls.find((field) => !field.checkValidity());
    if (!invalid) return true;
    invalid.reportValidity();
    invalid.focus({ preventScroll: true });
    invalid.scrollIntoView({ block: "center", behavior: "smooth" });
    return false;
  }

  setStep(step) {
    this.currentStep = clampStep(step, this.maxStep);
    this.form.dataset.draftCurrentStep = String(this.currentStep);
    this.renderSteps();
    document.dispatchEvent(new CustomEvent("guided-form:step-changed", {
      detail: { form: this.form, currentStep: this.currentStep },
    }));
  }

  renderSteps() {
    const hasWizardNavigation = Boolean(
      this.form.querySelector("[data-guided-next], [data-guided-previous]"),
    );
    const mobile = hasWizardNavigation && isMobileWizard(globalThis.innerWidth ?? 1024);
    this.form.querySelectorAll("[data-form-step]").forEach((section) => {
      const step = Number(section.dataset.formStep);
      section.hidden = mobile && step !== this.currentStep;
    });
    this.form.querySelectorAll("[data-guided-progress]").forEach((element) => {
      element.textContent = `Etapa ${this.currentStep} de ${this.maxStep}`;
    });
    this.form.querySelectorAll("[data-guided-previous]").forEach((button) => {
      button.disabled = this.currentStep <= 1;
    });
    this.form.querySelectorAll("[data-guided-next]").forEach((button) => {
      button.hidden = this.currentStep >= this.maxStep;
    });
    this.form.querySelectorAll("[data-guided-final]").forEach((element) => {
      element.hidden = mobile && this.currentStep < this.maxStep;
    });
  }

  async checkRecovery() {
    if (!this.type || !this.contextKey()) return;
    try {
      const draft = await this.client.load(this.type, this.contextKey());
      if (!draft) return;
      this.recoveryDraft = draft;
      this.populateRecovery(draft);
      this.showDialog("recovery");
    } catch {
      // Opening a final form remains possible when draft lookup fails.
    }
  }

  restore(draft) {
    for (const [name, value] of Object.entries(draft.payload ?? {})) {
      const fields = Array.from(this.form.elements).filter((field) => field.name === name);
      for (const field of fields) {
        if (field.type === "radio") {
          field.checked = String(field.value) === String(value);
        } else if (field.type === "checkbox" && Array.isArray(value)) {
          field.checked = value.map(String).includes(String(field.value));
        } else if (field.type === "checkbox") {
          field.checked = value === true || String(value) === "true";
        } else if (field.type !== "file") {
          field.value = value ?? "";
        }
      }
      if (Array.isArray(value) && fields[0]) {
        fields[0].dispatchEvent(new Event("change", { bubbles: true }));
      }
    }
    this.version = draft.version ?? null;
    this.lastContextKey = draft.contextKey ?? this.contextKey();
    this.form.dataset.draftContextKey = this.lastContextKey;
    this.form.dataset.draftVersion = this.version === null ? "" : String(this.version);
    this.setStep(draft.currentStep ?? 1);
    document.dispatchEvent(new CustomEvent("guided-form:restored", {
      detail: { form: this.form, draft },
    }));
  }

  async reconcileEmergencyCopy() {
    if (!this.type || !this.contextKey() || globalThis.navigator?.onLine === false) return;
    const emergency = this.client.readEmergency(this.type, this.contextKey());
    if (!emergency?.state) return;
    try {
      const server = await this.client.load(this.type, this.contextKey());
      if (server && Number(server.version) > Number(emergency.state.version ?? -1)) {
        this.pendingConflict = new DraftConflictError(
          "Este rascunho foi alterado em outro dispositivo.",
          server,
        );
        this.populateConflict(server);
        this.showDialog("conflict");
        return;
      }
      this.restore({
        ...emergency.state,
        payload: emergency.state.payload,
        version: emergency.state.version,
      });
      await this.save({ immediate: true });
    } catch {
      // Keep the emergency copy until a later successful reconciliation.
    }
  }

  setSaveStatus(text, state) {
    this.form.querySelectorAll("[data-guided-save-status]").forEach((element) => {
      element.textContent = text;
      element.dataset.saveState = state;
    });
  }

  populateRecovery(draft) {
    this.form.querySelectorAll("[data-draft-saved-at]").forEach((element) => {
      element.textContent = formatDateTime(draft.updatedAt);
    });
  }

  populateConflict(current) {
    this.form.querySelectorAll("[data-conflict-server-time]").forEach((element) => {
      element.textContent = formatDateTime(current?.updatedAt);
    });
    this.form.querySelectorAll("[data-conflict-local-time]").forEach((element) => {
      element.textContent = formatDateTime(new Date());
    });
  }

  showDialog(kind) {
    const element = this.form.querySelector(`[data-guided-dialog="${kind}"]`);
    if (!element) return;
    const modal = globalThis.bootstrap?.Modal?.getOrCreateInstance?.(element);
    if (modal) modal.show();
    else {
      element.hidden = false;
      element.setAttribute("aria-hidden", "false");
    }
  }

  hideDialog(kind) {
    const element = this.form.querySelector(`[data-guided-dialog="${kind}"]`);
    if (!element) return;
    const modal = globalThis.bootstrap?.Modal?.getInstance?.(element);
    if (modal) modal.hide();
    else {
      element.hidden = true;
      element.setAttribute("aria-hidden", "true");
    }
  }
}

export function connectGuidedForms(root = document) {
  const controllers = Array.from(root.querySelectorAll("form[data-guided-form]"))
    .map((form) => new GuidedFormController(form));
  controllers.forEach((controller) => controller.connect());
  return controllers;
}

if (typeof document !== "undefined") {
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => connectGuidedForms());
  } else {
    connectGuidedForms();
  }
}

function parseNullableNumber(value) {
  if (value === undefined || value === null || value === "") return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function formatTime(value) {
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return "--:--";
  return new Intl.DateTimeFormat("pt-BR", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function formatDateTime(value) {
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return "horário desconhecido";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}
