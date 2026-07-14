import {
  initializeLocalizedInputs,
  formatLocalizedInputs
} from './localized-inputs.js';
import { formatVehiclePlate } from './vehicle-form-inputs.js';

const setCaretToEnd = (input) => {
  if (typeof input.setSelectionRange !== 'function') return;
  const end = input.value.length;
  input.setSelectionRange(end, end);
};

const syncInputValidity = (input) => {
  if (!input) return;

  const invalidByServer = input.classList?.contains('is-invalid') ?? false;
  const invalidByBrowser = typeof input.checkValidity === 'function'
    ? !input.checkValidity()
    : false;
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

const markInvalidFields = (form, windowObject) => {
  const invalidInputs = Array.from(form.querySelectorAll(':invalid, .is-invalid'));
  invalidInputs.forEach(syncInputValidity);

  if (invalidInputs.length > 0) {
    windowObject.requestAnimationFrame(() => invalidInputs[0].focus());
  }

  return invalidInputs;
};

export const setVehicleSubmitState = (button, submitting) => {
  if (!button) return;

  if (submitting) {
    button.dataset.originalText ||= button.textContent;
    button.disabled = true;
    button.textContent = 'Salvando…';
    return;
  }

  button.disabled = false;
  if (button.dataset.originalText) {
    button.textContent = button.dataset.originalText;
  }
};

export const initializeVehicleForm = (
  documentObject = document,
  windowObject = window
) => {
  const form = documentObject.querySelector('#vehicleForm');
  if (!form) return null;

  const submitButton = form.querySelector('[data-vehicle-submit]');
  const plateInputs = Array.from(form.querySelectorAll('[data-vehicle-plate]'));
  let dirty = false;
  let submitting = false;

  initializeLocalizedInputs(form);

  const markInputChanged = (input) => {
    dirty = true;
    if (input?.classList?.contains('is-invalid') && input.checkValidity?.()) {
      input.classList.remove('is-invalid');
    }
    syncInputValidity(input);
  };

  plateInputs.forEach((input) => {
    input.value = formatVehiclePlate(input.value);
    input.addEventListener('input', () => {
      input.value = formatVehiclePlate(input.value);
      setCaretToEnd(input);
    });
  });

  Array.from(form.querySelectorAll('.is-invalid')).forEach(syncInputValidity);

  form.addEventListener('invalid', (event) => {
    syncInputValidity(event.target);
  }, true);

  form.addEventListener('input', (event) => {
    markInputChanged(event.target);
  });

  form.addEventListener('submit', (event) => {
    formatLocalizedInputs(form, { final: true });

    if (!form.checkValidity()) {
      event.preventDefault();
      submitting = false;
      setVehicleSubmitState(submitButton, false);
      markInvalidFields(form, windowObject);
      return;
    }

    submitting = true;
    dirty = false;
    setVehicleSubmitState(submitButton, true);
  });

  windowObject.addEventListener('pageshow', () => {
    submitting = false;
    setVehicleSubmitState(submitButton, false);
  });

  windowObject.addEventListener('beforeunload', (event) => {
    if (!dirty || submitting) return;
    event.preventDefault();
    event.returnValue = '';
  });

  return { form, submitButton };
};

if (typeof document !== 'undefined') {
  initializeVehicleForm(document, window);
}
