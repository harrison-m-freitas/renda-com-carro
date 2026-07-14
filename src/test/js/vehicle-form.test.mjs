import test from 'node:test';
import assert from 'node:assert/strict';

import { initializeVehicleForm } from '../../main/resources/static/js/vehicle-form.js';

const createClassList = (initial = []) => {
  const values = new Set(initial);
  return {
    add: (value) => values.add(value),
    remove: (value) => values.delete(value),
    contains: (value) => values.has(value)
  };
};

const createHarness = ({ valid }) => {
  let currentValid = valid;
  const formListeners = new Map();
  const windowListeners = new Map();
  const group = { classList: createClassList() };
  const invalidInput = {
    classList: createClassList(valid ? [] : ['is-invalid']),
    focused: false,
    attributes: new Map(),
    checkValidity: () => currentValid,
    closest: (selector) => selector === '.input-group' ? group : null,
    focus() { this.focused = true; },
    setAttribute(name, value) { this.attributes.set(name, value); },
    removeAttribute(name) { this.attributes.delete(name); }
  };
  const button = {
    disabled: false,
    textContent: 'Cadastrar veículo',
    dataset: {}
  };

  const selectorResults = new Map([
    ['[data-vehicle-plate]', []],
    ['[data-money-input]', []],
    ['[data-percentage-input]', []],
    ['[data-odometer-input]', []],
    ['[data-normalize-spaces]', []],
    ['[data-trim-outer-whitespace]', []],
    ['.is-invalid', valid ? [] : [invalidInput]],
    [':invalid, .is-invalid', valid ? [] : [invalidInput]]
  ]);

  const form = {
    checkValidity: () => currentValid,
    querySelector: (selector) => selector === '[data-vehicle-submit]' ? button : null,
    querySelectorAll: (selector) => selectorResults.get(selector) || [],
    addEventListener: (type, listener) => formListeners.set(type, listener)
  };

  const documentObject = {
    querySelector: (selector) => selector === '#vehicleForm' ? form : null
  };
  const windowObject = {
    addEventListener: (type, listener) => windowListeners.set(type, listener),
    requestAnimationFrame: (callback) => callback()
  };

  return {
    button,
    formListeners,
    invalidInput,
    windowListeners,
    documentObject,
    windowObject,
    setValid(value) { currentValid = value; }
  };
};

test('vehicle form: invalid submit keeps button enabled and focuses first invalid field', () => {
  const harness = createHarness({ valid: false });
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  let prevented = false;
  harness.formListeners.get('submit')({
    preventDefault() { prevented = true; }
  });

  assert.equal(prevented, true);
  assert.equal(harness.button.disabled, false);
  assert.equal(harness.button.textContent, 'Cadastrar veículo');
  assert.equal(harness.invalidInput.focused, true);
  assert.equal(harness.invalidInput.attributes.get('aria-invalid'), 'true');
});

test('vehicle form: pageshow restores submit button after a valid submission', () => {
  const harness = createHarness({ valid: true });
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  harness.formListeners.get('submit')({});
  assert.equal(harness.button.disabled, true);
  assert.equal(harness.button.textContent, 'Salvando…');

  harness.windowListeners.get('pageshow')({});
  assert.equal(harness.button.disabled, false);
  assert.equal(harness.button.textContent, 'Cadastrar veículo');
});

test('vehicle form: corrected server error clears invalid state on input', () => {
  const harness = createHarness({ valid: false });
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  harness.setValid(true);
  harness.formListeners.get('input')({ target: harness.invalidInput });

  assert.equal(harness.invalidInput.classList.contains('is-invalid'), false);
  assert.equal(harness.invalidInput.attributes.has('aria-invalid'), false);
});

test('vehicle form: user input marks the page dirty', () => {
  const harness = createHarness({ valid: true });
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  harness.formListeners.get('input')({ target: harness.invalidInput });

  let unloadPrevented = false;
  harness.windowListeners.get('beforeunload')({
    preventDefault() { unloadPrevented = true; },
    returnValue: null
  });
  assert.equal(unloadPrevented, true);
});
