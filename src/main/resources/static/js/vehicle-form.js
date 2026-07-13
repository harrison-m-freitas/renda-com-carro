import {
  applyMoneyEdit,
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

const replacesEntireValue = (input) => input.value.length > 0
  && input.selectionStart === 0
  && input.selectionEnd === input.value.length;

const renderMoneyEdit = (input, edit, maxDigits) => {
  const previousDigits = input.dataset.moneyDigits || '';
  const digits = applyMoneyEdit(previousDigits, {
    ...edit,
    maxDigits
  });
  input.dataset.moneyDigits = digits;
  input.value = formatMoneyInput(digits, maxDigits);
  setCaretToEnd(input);
  return digits !== previousDigits;
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

  moneyInputs.forEach((input) => {
    const maxDigits = Number(input.dataset.maxDigits || '14');
    input.dataset.moneyDigits = String(input.value ?? '').replace(/\D/g, '').slice(0, maxDigits);
    input.value = formatMoneyInput(input.dataset.moneyDigits, maxDigits);

    input.addEventListener('beforeinput', (event) => {
      const inputType = event.inputType || '';
      if (inputType.startsWith('delete')) {
        event.preventDefault();
        const changed = renderMoneyEdit(input, {
          inputType,
          replaceAll: replacesEntireValue(input)
        }, maxDigits);
        if (changed) markInputChanged(input);
        return;
      }

      if (!inputType.startsWith('insert') || inputType === 'insertFromPaste') return;
      event.preventDefault();
      const changed = renderMoneyEdit(input, {
        inputType,
        data: event.data || '',
        replaceAll: replacesEntireValue(input)
      }, maxDigits);
      if (changed) markInputChanged(input);
    });

    input.addEventListener('paste', (event) => {
      event.preventDefault();
      const changed = renderMoneyEdit(input, {
        inputType: 'insertFromPaste',
        data: event.clipboardData?.getData('text') || '',
        replaceAll: replacesEntireValue(input)
      }, maxDigits);
      if (changed) markInputChanged(input);
    });

    input.addEventListener('input', (event) => {
      const inputType = event.inputType || '';
      if (inputType.startsWith('delete') || inputType.startsWith('insert')) {
        renderMoneyEdit(input, {
          inputType,
          data: event.data || '',
          replaceAll: replacesEntireValue(input)
        }, maxDigits);
        return;
      }

      input.dataset.moneyDigits = input.value.replace(/\D/g, '').slice(0, maxDigits);
      input.value = formatMoneyInput(input.dataset.moneyDigits, maxDigits);
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
    markInputChanged(event.target);
  });

  form.addEventListener('submit', (event) => {
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
