import {
  formatMoneyInput,
  formatOdometerInput,
  formatVehiclePlate,
  normalizeVehicleText
} from './vehicle-form-inputs.js';

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
  const moneyInputs = Array.from(form.querySelectorAll('[data-money-input]'));
  const odometerInputs = Array.from(form.querySelectorAll('[data-odometer-input]'));
  const textInputs = Array.from(form.querySelectorAll('[data-normalize-spaces]'));
  let dirty = false;
  let submitting = false;

  plateInputs.forEach((input) => {
    input.value = formatVehiclePlate(input.value);
    input.addEventListener('input', () => {
      input.value = formatVehiclePlate(input.value);
      setCaretToEnd(input);
    });
  });

  moneyInputs.forEach((input) => {
    input.value = formatMoneyInput(
      input.value,
      Number(input.dataset.maxDigits || '14')
    );
    input.addEventListener('input', () => {
      input.value = formatMoneyInput(
        input.value,
        Number(input.dataset.maxDigits || '14')
      );
      setCaretToEnd(input);
    });
  });

  odometerInputs.forEach((input) => {
    const maxIntegerDigits = Number(input.dataset.maxIntegerDigits || '11');
    input.value = formatOdometerInput(input.value, {
      maxIntegerDigits,
      trimZeroFraction: true
    });
    input.addEventListener('input', () => {
      input.value = formatOdometerInput(input.value, { maxIntegerDigits });
      setCaretToEnd(input);
    });
    input.addEventListener('blur', () => {
      input.value = formatOdometerInput(input.value, {
        maxIntegerDigits,
        trimZeroFraction: true
      });
    });
  });

  textInputs.forEach((input) => {
    input.addEventListener('blur', () => {
      input.value = normalizeVehicleText(input.value);
    });
  });

  Array.from(form.querySelectorAll('.is-invalid')).forEach(syncInputValidity);

  form.addEventListener('invalid', (event) => {
    syncInputValidity(event.target);
  }, true);

  form.addEventListener('input', (event) => {
    dirty = true;
    syncInputValidity(event.target);
  });

  form.addEventListener('submit', () => {
    textInputs.forEach((input) => {
      input.value = normalizeVehicleText(input.value);
    });
    odometerInputs.forEach((input) => {
      input.value = formatOdometerInput(input.value, {
        maxIntegerDigits: Number(input.dataset.maxIntegerDigits || '11'),
        trimZeroFraction: true
      });
    });

    if (!form.checkValidity()) {
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
