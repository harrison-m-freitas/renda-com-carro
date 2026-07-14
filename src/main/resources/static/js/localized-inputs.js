import {
  applyFixedScaleEdit,
  formatMoneyInput,
  formatPercentageInput,
  formatOdometerInput,
  parseLocalizedDecimal,
  normalizeSpaces,
  trimOuterWhitespace
} from './localized-input-formatters.js';

const initializedInputs = new WeakSet();
const initializedSubmitRoots = new WeakSet();
const restoredListenerDocuments = new WeakSet();
const syntheticInputTargets = new WeakSet();

const asArray = (value) => Array.from(value ?? []);

const matchingInputs = (root, selector) => {
  const matches = [];
  if (root?.matches?.(selector)) matches.push(root);
  if (root?.querySelectorAll) matches.push(...asArray(root.querySelectorAll(selector)));
  return matches;
};

const setCaretToEnd = (input) => {
  if (typeof input.setSelectionRange !== 'function') return;
  const end = input.value.length;
  input.setSelectionRange(end, end);
};

const selectedDigitRange = (input) => {
  const selectionStart = Number(input.selectionStart);
  const selectionEnd = Number(input.selectionEnd);
  if (!Number.isInteger(selectionStart) || !Number.isInteger(selectionEnd)
      || selectionEnd <= selectionStart) {
    return { selectionStart: null, selectionEnd: null, replaceAll: false };
  }

  const value = String(input.value ?? '');
  const digitOffset = (offset) => value.slice(0, offset).replace(/\D/g, '').length;
  return {
    selectionStart: digitOffset(selectionStart),
    selectionEnd: digitOffset(selectionEnd),
    replaceAll: selectionStart === 0 && selectionEnd === value.length
  };
};

const eventConstructorFor = (input) => input?.ownerDocument?.defaultView?.Event
  ?? globalThis.Event;

const dispatchUserInput = (input) => {
  if (typeof input.dispatchEvent !== 'function') return;
  const EventConstructor = eventConstructorFor(input);
  if (typeof EventConstructor !== 'function') return;

  syntheticInputTargets.add(input);
  try {
    input.dispatchEvent(new EventConstructor('input', { bubbles: true }));
  } finally {
    syntheticInputTargets.delete(input);
  }
};

const replaceValueAfterUserEdit = (input, nextValue) => {
  if (nextValue === input.value) return false;
  input.value = nextValue;
  dispatchUserInput(input);
  return true;
};

const validatePercentageInput = (input) => {
  const maximum = Number(input.dataset?.maxValue || '100');
  const value = parseLocalizedDecimal(input.value);
  const invalid = value !== null && (value < 0 || value > maximum);
  input.setCustomValidity?.(
    invalid ? `Informe um percentual entre 0 e ${maximum}.` : ''
  );
};

const configureFixedScaleInput = (
  input,
  formatter,
  defaultMaxDigits,
  afterRender = null
) => {
  if (initializedInputs.has(input)) return false;
  initializedInputs.add(input);
  if (input.dataset) input.dataset.localizedInputInitialized = 'true';

  const maxDigits = Number(input.dataset?.maxDigits || defaultMaxDigits);

  const renderDigits = (digits) => {
    const normalizedDigits = String(digits ?? '').replace(/\D/g, '').slice(0, maxDigits);
    if (input.dataset) input.dataset.localizedDigits = normalizedDigits;
    input.value = formatter(normalizedDigits, maxDigits);
    afterRender?.(input);
    setCaretToEnd(input);
    return normalizedDigits;
  };

  renderDigits(input.value);

  input.addEventListener('beforeinput', (event) => {
    const inputType = event.inputType || '';
    const isDeletion = inputType.startsWith('delete');
    const isInsertion = inputType.startsWith('insert') && inputType !== 'insertFromPaste';
    if (!isDeletion && !isInsertion) return;

    event.preventDefault();
    const previousDigits = input.dataset?.localizedDigits || '';
    const selection = selectedDigitRange(input);
    const nextDigits = applyFixedScaleEdit(previousDigits, {
      inputType,
      data: event.data || '',
      ...selection,
      maxDigits
    });
    if (nextDigits === previousDigits) return;

    renderDigits(nextDigits);
    dispatchUserInput(input);
  });

  input.addEventListener('paste', (event) => {
    event.preventDefault();
    const previousDigits = input.dataset?.localizedDigits || '';
    const selection = selectedDigitRange(input);
    const nextDigits = applyFixedScaleEdit(previousDigits, {
      inputType: 'insertFromPaste',
      data: event.clipboardData?.getData('text') || '',
      ...selection,
      maxDigits
    });
    if (nextDigits === previousDigits) return;

    renderDigits(nextDigits);
    dispatchUserInput(input);
  });

  input.addEventListener('input', () => {
    if (syntheticInputTargets.has(input)) return;
    renderDigits(input.value);
  });

  return true;
};

const configureOdometerInput = (input) => {
  if (initializedInputs.has(input)) return false;
  initializedInputs.add(input);
  if (input.dataset) input.dataset.localizedInputInitialized = 'true';

  const maxIntegerDigits = Number(input.dataset?.maxIntegerDigits || '11');
  input.value = formatOdometerInput(input.value, {
    maxIntegerDigits,
    trimZeroFraction: true
  });

  input.addEventListener('input', () => {
    input.value = formatOdometerInput(input.value, { maxIntegerDigits });
    setCaretToEnd(input);
  });

  input.addEventListener('blur', () => {
    replaceValueAfterUserEdit(input, formatOdometerInput(input.value, {
      maxIntegerDigits,
      trimZeroFraction: true
    }));
  });

  return true;
};

const configureNormalizedTextInput = (input) => {
  if (initializedInputs.has(input)) return false;
  initializedInputs.add(input);
  if (input.dataset) input.dataset.localizedInputInitialized = 'true';

  input.addEventListener('blur', () => {
    replaceValueAfterUserEdit(input, normalizeSpaces(input.value));
  });
  return true;
};

const configureOuterTrimInput = (input) => {
  if (initializedInputs.has(input)) return false;
  initializedInputs.add(input);
  if (input.dataset) input.dataset.localizedInputInitialized = 'true';

  input.addEventListener('blur', () => {
    replaceValueAfterUserEdit(input, trimOuterWhitespace(input.value));
  });
  return true;
};

export const formatLocalizedInputs = (root, { final = false } = {}) => {
  matchingInputs(root, '[data-money-input]').forEach((input) => {
    const maxDigits = Number(input.dataset?.maxDigits || '14');
    const digits = String(input.value ?? '').replace(/\D/g, '').slice(0, maxDigits);
    if (input.dataset) input.dataset.localizedDigits = digits;
    input.value = formatMoneyInput(digits, maxDigits);
  });

  matchingInputs(root, '[data-percentage-input]').forEach((input) => {
    const maxDigits = Number(input.dataset?.maxDigits || '5');
    const digits = String(input.value ?? '').replace(/\D/g, '').slice(0, maxDigits);
    if (input.dataset) input.dataset.localizedDigits = digits;
    input.value = formatPercentageInput(digits, maxDigits);
    validatePercentageInput(input);
  });

  matchingInputs(root, '[data-odometer-input]').forEach((input) => {
    input.value = formatOdometerInput(input.value, {
      maxIntegerDigits: Number(input.dataset?.maxIntegerDigits || '11'),
      trimZeroFraction: final
    });
  });

  if (final) {
    matchingInputs(root, '[data-normalize-spaces]').forEach((input) => {
      input.value = normalizeSpaces(input.value);
    });
    matchingInputs(root, '[data-trim-outer-whitespace]').forEach((input) => {
      input.value = trimOuterWhitespace(input.value);
    });
  }
};

const installRestoreListener = (root) => {
  const documentObject = root?.nodeType === 9 ? root : root?.ownerDocument;
  if (!documentObject?.addEventListener || restoredListenerDocuments.has(documentObject)) return;

  restoredListenerDocuments.add(documentObject);
  documentObject.addEventListener('guided-form:restored', (event) => {
    const restoredForm = event.detail?.form;
    if (!restoredForm) return;
    initializeLocalizedInputs(restoredForm);
    formatLocalizedInputs(restoredForm);
  });
};

const installSubmitNormalization = (root) => {
  if (!root?.addEventListener || initializedSubmitRoots.has(root)) return;
  const isForm = String(root.tagName ?? '').toUpperCase() === 'FORM'
    || root.matches?.('form');
  if (!isForm) return;

  initializedSubmitRoots.add(root);
  root.addEventListener('submit', () => {
    formatLocalizedInputs(root, { final: true });
  });
};

export const initializeLocalizedInputs = (root = document) => {
  let initialized = 0;

  matchingInputs(root, '[data-money-input]').forEach((input) => {
    if (configureFixedScaleInput(input, formatMoneyInput, 14)) initialized += 1;
  });
  matchingInputs(root, '[data-percentage-input]').forEach((input) => {
    if (configureFixedScaleInput(
      input,
      formatPercentageInput,
      5,
      validatePercentageInput
    )) initialized += 1;
  });
  matchingInputs(root, '[data-odometer-input]').forEach((input) => {
    if (configureOdometerInput(input)) initialized += 1;
  });
  matchingInputs(root, '[data-normalize-spaces]').forEach((input) => {
    if (configureNormalizedTextInput(input)) initialized += 1;
  });
  matchingInputs(root, '[data-trim-outer-whitespace]').forEach((input) => {
    if (configureOuterTrimInput(input)) initialized += 1;
  });

  installRestoreListener(root);
  installSubmitNormalization(root);
  return { initialized };
};
