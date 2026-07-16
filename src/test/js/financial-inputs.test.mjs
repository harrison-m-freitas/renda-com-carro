import test from 'node:test';
import assert from 'node:assert/strict';
import { initializeFinancialInputs } from '../../main/resources/static/js/financial-inputs.js';

function harness(value = '', kind = 'money') {
  const listeners = new Map();
  const input = {
    value,
    dataset: {},
    selectionStart: value.length,
    matches(selector) {
      return kind === 'percent'
        ? selector === '[data-financial-percent]'
        : selector === '[data-financial-money]';
    },
    addEventListener(type, listener) {
      const registered = listeners.get(type) ?? [];
      registered.push(listener);
      listeners.set(type, registered);
    },
    setSelectionRange(start) {
      this.selectionStart = start;
    },
  };
  const root = {
    querySelectorAll() {
      return [input];
    },
  };
  return { input, root, listeners };
}

test('financial inputs reformat a value restored after initialization without duplicate listeners', () => {
  const { input, root, listeners } = harness();
  initializeFinancialInputs(root);
  input.value = '35000.00';

  initializeFinancialInputs(root);

  assert.equal(input.value, '35.000,00');
  assert.equal(listeners.get('input').length, 1);
  assert.equal(listeners.get('blur').length, 1);
});


test('financial inputs restore canonical percentages without treating dots as thousands', () => {
  const { input, root } = harness('0.005', 'percent');
  input.dataset.financialCanonical = 'true';

  initializeFinancialInputs(root);

  assert.equal(input.value, '0,005');
  assert.equal(input.dataset.financialCanonical, undefined);
});
