import {
  initializeLocalizedInputs,
  formatLocalizedInputs
} from './localized-inputs.js';
import {
  formatVehiclePlate,
  formatVehiclePlateEdit
} from './vehicle-form-inputs.js';

export const MOBILE_VEHICLE_QUERY = '(max-width: 767.98px)';

const STEP_ORDER = ['identification', 'operation'];
const STEP_LABELS = {
  identification: 'Identificação',
  operation: 'Operação'
};

const NON_VALUE_CONTROL_TYPES = new Set(['button', 'submit', 'reset', 'image']);

export const serializeVehicleForm = (form) => JSON.stringify(
  Array.from(form?.elements ?? [])
    .filter((control) => (
      control?.name
      && !control.disabled
      && !NON_VALUE_CONTROL_TYPES.has(String(control.type ?? '').toLowerCase())
    ))
    .map((control) => {
      const type = String(control.type ?? '').toLowerCase();
      const value = type === 'checkbox' || type === 'radio'
        ? Boolean(control.checked)
        : String(control.value ?? '');
      return [control.name, value];
    })
);

const syncInputValidity = (input, browserInvalidOverride = null) => {
  if (!input) return;

  const invalidByServer = input.classList?.contains('is-invalid') ?? false;
  const invalidByBrowser = browserInvalidOverride ?? (
    typeof input.checkValidity === 'function'
      ? !input.checkValidity()
      : false
  );
  const invalid = invalidByServer || invalidByBrowser;
  const group = input.closest?.('.input-group');

  if (invalid) {
    input.setAttribute?.('aria-invalid', 'true');
    group?.classList.add('is-invalid-group');
    return;
  }

  input.removeAttribute?.('aria-invalid');
  input.classList?.remove('is-invalid');
  group?.classList.remove('is-invalid-group');
};

const collectInvalidFields = (form) => {
  const invalidInputs = Array.from(form.querySelectorAll(':invalid, .is-invalid'));
  invalidInputs.forEach((input) => syncInputValidity(input));
  return invalidInputs;
};

const getStepControls = (step) => Array.from(
  step?.querySelectorAll?.('input, select, textarea') ?? []
).filter((control) => !control.disabled);

export const validateVehicleStep = (step) => {
  const invalidInputs = getStepControls(step)
    .filter((control) => !control.checkValidity());

  invalidInputs.forEach((input) => syncInputValidity(input));
  return invalidInputs;
};

export const setVehicleSubmitState = (button, submitting) => {
  if (!button) return;

  const spinner = button.querySelector?.('[data-vehicle-submit-spinner]');
  const copy = button.querySelector?.('[data-vehicle-submit-copy]');
  const textTarget = copy ?? button;

  if (submitting) {
    textTarget.dataset.originalText ||= textTarget.textContent;
    button.disabled = true;
    textTarget.textContent = 'Salvando…';
    if (spinner) spinner.hidden = false;
    return;
  }

  button.disabled = false;
  if (textTarget.dataset.originalText) {
    textTarget.textContent = textTarget.dataset.originalText;
  }
  if (spinner) spinner.hidden = true;
};

export const initializeVehicleForm = (
  documentObject = document,
  windowObject = window
) => {
  const form = documentObject.querySelector('#vehicleForm');
  if (!form) return null;

  const submitButton = form.querySelector('[data-vehicle-submit]');
  const plateInputs = Array.from(form.querySelectorAll('[data-vehicle-plate]'));
  const steps = new Map(
    Array.from(form.querySelectorAll('[data-vehicle-step]'))
      .map((step) => [step.dataset.vehicleStep, step])
  );
  const progress = form.querySelector('[data-vehicle-progress]');
  const progressCopy = form.querySelector('[data-vehicle-progress-copy]');
  const progressName = form.querySelector('[data-vehicle-progress-name]');
  const progressBar = form.querySelector('[data-vehicle-progress-bar]');
  const progressRole = progress?.querySelector?.('[role="progressbar"]');
  const previousButton = form.querySelector('[data-vehicle-previous]');
  const nextButton = form.querySelector('[data-vehicle-next]');
  const cancelAction = form.querySelector('[data-vehicle-cancel-action]');
  const cancelLinks = Array.from(
    documentObject.querySelectorAll?.('[data-vehicle-cancel]')
    ?? form.querySelectorAll('[data-vehicle-cancel]')
  );
  const acquisitionToggle = form.querySelector('[data-vehicle-acquisition-toggle]');
  const acquisitionPanel = form.querySelector('[data-vehicle-acquisition-panel]');
  const acquisitionIcon = acquisitionToggle?.querySelector?.('[data-vehicle-acquisition-icon]');
  const nameHelp = form.querySelector('[data-vehicle-name-help]');
  const namedControls = Array.from(form.elements ?? []);
  const findNamedControl = (name) => namedControls.find((control) => control.name === name);
  const makeInput = findNamedControl('make');
  const modelInput = findNamedControl('model');
  const purchasePriceInput = findNamedControl('purchasePrice');
  const mediaQuery = windowObject.matchMedia?.(MOBILE_VEHICLE_QUERY) ?? {
    matches: false,
    addEventListener() {}
  };
  let currentStep = STEP_ORDER[0];
  let acquisitionExpanded = Boolean(
    String(purchasePriceInput?.value ?? '').trim()
    || purchasePriceInput?.classList?.contains('is-invalid')
  );
  let submitting = false;
  let discardConfirmed = false;
  let initialFormState = '';

  initializeLocalizedInputs(form);

  const markInputChanged = (input) => {
    if (input?.classList?.contains('is-invalid') && input.checkValidity?.()) {
      input.classList.remove('is-invalid');
    }
    syncInputValidity(input);
  };

  plateInputs.forEach((input) => {
    input.value = formatVehiclePlate(input.value);
    input.addEventListener('input', () => {
      const edit = formatVehiclePlateEdit(
        input.value,
        input.selectionStart,
        input.selectionEnd
      );
      input.value = edit.value;
      input.setSelectionRange?.(edit.selectionStart, edit.selectionEnd);
    });
  });

  initialFormState = serializeVehicleForm(form);
  const isDirty = () => serializeVehicleForm(form) !== initialFormState;

  const isMobile = () => Boolean(mediaQuery.matches);

  const syncAcquisition = () => {
    const mobile = isMobile();
    if (acquisitionToggle) acquisitionToggle.hidden = !mobile;
    if (acquisitionPanel) acquisitionPanel.hidden = mobile && !acquisitionExpanded;
    acquisitionToggle?.setAttribute?.('aria-expanded', String(mobile ? acquisitionExpanded : true));
    if (acquisitionIcon) acquisitionIcon.textContent = acquisitionExpanded ? '▾' : '▸';
  };

  const setAcquisitionExpanded = (expanded) => {
    acquisitionExpanded = Boolean(expanded);
    syncAcquisition();
  };

  const updateNameHelp = () => {
    if (!nameHelp) return;

    const identity = [makeInput?.value, modelInput?.value]
      .map((value) => String(value ?? '').trim())
      .filter(Boolean)
      .join(' ');

    nameHelp.textContent = identity
      ? `Se ficar vazio, o veículo será exibido como ${identity}.`
      : 'Se ficar vazio, usaremos marca e modelo como identificação.';
  };

  const syncFlow = () => {
    const mobile = isMobile();
    form.classList?.toggle('vehicle-form--enhanced', mobile);

    STEP_ORDER.forEach((stepName) => {
      const step = steps.get(stepName);
      if (!step) return;

      const active = stepName === currentStep;
      step.hidden = mobile ? !active : false;
      if (mobile && active) {
        step.setAttribute?.('aria-current', 'step');
      } else {
        step.removeAttribute?.('aria-current');
      }
    });

    if (progress) progress.hidden = !mobile;
    if (previousButton) {
      previousButton.hidden = !mobile || currentStep !== 'operation';
    }
    if (nextButton) {
      nextButton.hidden = !mobile || currentStep !== 'identification';
    }
    if (submitButton) {
      submitButton.hidden = mobile && currentStep !== 'operation';
    }
    if (cancelAction) {
      cancelAction.hidden = mobile && currentStep === 'operation';
    }

    const stepIndex = STEP_ORDER.indexOf(currentStep);
    const stepNumber = Math.max(stepIndex, 0) + 1;
    if (progressCopy) progressCopy.textContent = `Etapa ${stepNumber} de ${STEP_ORDER.length}`;
    if (progressName) progressName.textContent = STEP_LABELS[currentStep];
    if (progressBar) progressBar.style.width = `${stepNumber * 50}%`;
    progressRole?.setAttribute?.('aria-valuenow', String(stepNumber));
    syncAcquisition();
  };

  const showStep = (stepName, { focusHeading = false } = {}) => {
    if (!STEP_ORDER.includes(stepName) || !steps.has(stepName)) return false;

    currentStep = stepName;
    syncFlow();

    if (focusHeading && isMobile()) {
      const heading = steps.get(stepName)?.querySelector?.('[data-vehicle-step-heading]');
      windowObject.requestAnimationFrame(() => heading?.focus?.());
    }

    return true;
  };

  nextButton?.addEventListener('click', () => {
    const identificationStep = steps.get('identification');
    const stepError = identificationStep?.querySelector?.('[data-vehicle-step-error]');
    const invalidInputs = validateVehicleStep(identificationStep);

    if (stepError) stepError.hidden = invalidInputs.length === 0;
    if (invalidInputs.length > 0) {
      const firstInvalid = invalidInputs[0];
      firstInvalid.reportValidity?.();
      windowObject.requestAnimationFrame(() => {
        firstInvalid.focus?.();
        firstInvalid.scrollIntoView?.({ block: 'center', behavior: 'smooth' });
      });
      return;
    }

    showStep('operation', { focusHeading: true });
  });

  previousButton?.addEventListener('click', () => {
    showStep('identification', { focusHeading: true });
  });

  acquisitionToggle?.addEventListener('click', () => {
    setAcquisitionExpanded(!acquisitionExpanded);
  });

  cancelLinks.forEach((link) => {
    link.addEventListener('click', (event) => {
      if (!isDirty()) return;

      const confirmed = windowObject.confirm?.(
        'Descartar as alterações deste veículo?'
      ) ?? true;
      if (!confirmed) {
        event.preventDefault();
        return;
      }

      discardConfirmed = true;
    });
  });

  const serverInvalidInputs = Array.from(form.querySelectorAll('.is-invalid'));
  const firstServerInvalid = serverInvalidInputs[0];
  const invalidStepName = firstServerInvalid
    ?.closest?.('[data-vehicle-step]')
    ?.dataset?.vehicleStep;
  if (STEP_ORDER.includes(invalidStepName)) currentStep = invalidStepName;
  if (purchasePriceInput?.classList?.contains('is-invalid')) {
    acquisitionExpanded = true;
  }

  mediaQuery.addEventListener?.('change', syncFlow);
  updateNameHelp();
  syncFlow();

  if (firstServerInvalid && isMobile()) {
    windowObject.requestAnimationFrame(() => {
      firstServerInvalid.focus?.();
      firstServerInvalid.scrollIntoView?.({ block: 'center', behavior: 'smooth' });
    });
  }

  serverInvalidInputs.forEach((input) => syncInputValidity(input));

  form.addEventListener('invalid', (event) => {
    syncInputValidity(event.target, true);

    const firstInvalid = form.querySelector(':invalid, .is-invalid') ?? event.target;
    const invalidStep = firstInvalid
      ?.closest?.('[data-vehicle-step]')
      ?.dataset?.vehicleStep;

    if (firstInvalid === purchasePriceInput || firstInvalid?.name === 'purchasePrice') {
      setAcquisitionExpanded(true);
    }
    if (isMobile() && STEP_ORDER.includes(invalidStep)) {
      showStep(invalidStep);
    }

    const identificationError = steps.get('identification')
      ?.querySelector?.('[data-vehicle-step-error]');
    if (identificationError && invalidStep === 'identification') {
      identificationError.hidden = false;
    }

    windowObject.requestAnimationFrame(() => {
      firstInvalid?.focus?.();
      firstInvalid?.scrollIntoView?.({ block: 'center', behavior: 'smooth' });
    });
  }, true);

  form.addEventListener('input', (event) => {
    markInputChanged(event.target);
    if (event.target === makeInput || event.target === modelInput) {
      updateNameHelp();
    }
  });

  form.addEventListener('submit', (event) => {
    formatLocalizedInputs(form, { final: true });

    if (!form.checkValidity()) {
      event.preventDefault();
      submitting = false;
      setVehicleSubmitState(submitButton, false);
      const invalidInputs = collectInvalidFields(form);
      const firstInvalid = invalidInputs[0];
      const invalidStep = firstInvalid
        ?.closest?.('[data-vehicle-step]')
        ?.dataset?.vehicleStep;

      if (firstInvalid === purchasePriceInput || firstInvalid?.name === 'purchasePrice') {
        setAcquisitionExpanded(true);
      }
      if (isMobile() && STEP_ORDER.includes(invalidStep)) {
        showStep(invalidStep);
      }

      const identificationError = steps.get('identification')
        ?.querySelector?.('[data-vehicle-step-error]');
      if (identificationError && invalidStep === 'identification') {
        identificationError.hidden = false;
      }

      if (firstInvalid) {
        firstInvalid.reportValidity?.();
        windowObject.requestAnimationFrame(() => {
          firstInvalid.focus?.();
          firstInvalid.scrollIntoView?.({ block: 'center', behavior: 'smooth' });
        });
      }
      return;
    }

    submitting = true;
    setVehicleSubmitState(submitButton, true);
  });

  windowObject.addEventListener('pageshow', () => {
    submitting = false;
    discardConfirmed = false;
    setVehicleSubmitState(submitButton, false);
  });

  windowObject.addEventListener('beforeunload', (event) => {
    if (!isDirty() || submitting || discardConfirmed) return;
    event.preventDefault();
    event.returnValue = '';
  });

  return {
    form,
    submitButton,
    getCurrentStep: () => currentStep,
    showStep,
    setAcquisitionExpanded,
    isDirty
  };
};

if (typeof document !== 'undefined') {
  initializeVehicleForm(document, window);
}
