import test from 'node:test';
import assert from 'node:assert/strict';

import {
  initializeLocalizedInputs,
  formatLocalizedInputs
} from '../../main/resources/static/js/localized-inputs.js';

class FakeEvent {
  constructor(type, options = {}) {
    this.type = type;
    this.bubbles = Boolean(options.bubbles);
    this.inputType = options.inputType || '';
    this.data = options.data ?? null;
    this.defaultPrevented = false;
  }

  preventDefault() {
    this.defaultPrevented = true;
  }
}

const createHarness = ({
  attribute = 'data-money-input',
  value = '',
  dataset = {}
} = {}) => {
  const inputListeners = new Map();
  const formListeners = new Map();
  const documentListeners = new Map();
  const dispatched = [];

  const documentObject = {
    nodeType: 9,
    defaultView: { Event: FakeEvent },
    addEventListener(type, listener) {
      const values = documentListeners.get(type) || [];
      values.push(listener);
      documentListeners.set(type, values);
    }
  };

  const input = {
    tagName: 'INPUT',
    value,
    dataset: { ...dataset },
    customValidity: '',
    selectionStart: value.length,
    selectionEnd: value.length,
    ownerDocument: documentObject,
    matches(selector) {
      return selector === `[${attribute}]`;
    },
    querySelectorAll() {
      return [];
    },
    addEventListener(type, listener) {
      const values = inputListeners.get(type) || [];
      values.push(listener);
      inputListeners.set(type, values);
    },
    setSelectionRange(start, end) {
      this.selectionStart = start;
      this.selectionEnd = end;
    },
    setCustomValidity(message) {
      this.customValidity = message;
    },
    dispatchEvent(event) {
      dispatched.push(event);
      for (const listener of inputListeners.get(event.type) || []) listener(event);
      if (event.bubbles) {
        for (const listener of formListeners.get(event.type) || []) {
          listener({ ...event, target: input });
        }
      }
      return !event.defaultPrevented;
    }
  };

  const form = {
    tagName: 'FORM',
    ownerDocument: documentObject,
    matches(selector) {
      return selector === 'form';
    },
    querySelectorAll(selector) {
      return input.matches(selector) ? [input] : [];
    },
    addEventListener(type, listener) {
      const values = formListeners.get(type) || [];
      values.push(listener);
      formListeners.set(type, values);
    }
  };

  const fireInputListener = (type, event) => {
    for (const listener of inputListeners.get(type) || []) listener(event);
  };

  return {
    input,
    form,
    dispatched,
    listenerCount(type) {
      return (inputListeners.get(type) || []).length;
    },
    beforeInput(inputType, data = null) {
      const event = new FakeEvent('beforeinput');
      event.inputType = inputType;
      event.data = data;
      fireInputListener('beforeinput', event);
      return event;
    },
    restore() {
      for (const listener of documentListeners.get('guided-form:restored') || []) {
        listener({ detail: { form } });
      }
    },
    submit() {
      for (const listener of formListeners.get('submit') || []) listener({ target: form });
    }
  };
};

test('localized inputs: initializer is idempotent', () => {
  const harness = createHarness();
  assert.equal(initializeLocalizedInputs(harness.form).initialized, 1);
  assert.equal(initializeLocalizedInputs(harness.form).initialized, 0);
  assert.equal(harness.listenerCount('beforeinput'), 1);
});

test('localized inputs: money beforeinput renders and emits one bubbling input', () => {
  const harness = createHarness();
  initializeLocalizedInputs(harness.form);

  const event = harness.beforeInput('insertText', '1');

  assert.equal(event.defaultPrevented, true);
  assert.equal(harness.input.value, '0,01');
  assert.equal(harness.dispatched.filter((item) => item.type === 'input').length, 1);
});

test('localized inputs: restored value is formatted without synthetic input', () => {
  const harness = createHarness({ value: '' });
  initializeLocalizedInputs(harness.form);
  harness.dispatched.length = 0;
  harness.input.value = '123';

  harness.restore();

  assert.equal(harness.input.value, '1,23');
  assert.equal(harness.dispatched.length, 0);
});

test('localized inputs: percentage and odometer use their own formatters', () => {
  const percentage = createHarness({ attribute: 'data-percentage-input', value: '125' });
  initializeLocalizedInputs(percentage.form);
  assert.equal(percentage.input.value, '1,25');

  const odometer = createHarness({ attribute: 'data-odometer-input', value: '248351,5' });
  initializeLocalizedInputs(odometer.form);
  assert.equal(odometer.input.value, '248.351,5');
});

test('localized inputs: percentage keeps the typed value and reports values above one hundred', () => {
  const percentage = createHarness({
    attribute: 'data-percentage-input',
    value: '10001',
    dataset: { maxValue: '100' }
  });

  initializeLocalizedInputs(percentage.form);

  assert.equal(percentage.input.value, '100,01');
  assert.equal(percentage.input.customValidity, 'Informe um percentual entre 0 e 100.');

  percentage.input.value = '10000';
  formatLocalizedInputs(percentage.form);
  assert.equal(percentage.input.value, '100,00');
  assert.equal(percentage.input.customValidity, '');
});

test('localized inputs: final formatting trims normalized text', () => {
  const harness = createHarness({ attribute: 'data-normalize-spaces', value: '  Banco   Familiar  ' });
  initializeLocalizedInputs(harness.form);
  formatLocalizedInputs(harness.form, { final: true });
  assert.equal(harness.input.value, 'Banco Familiar');
});
