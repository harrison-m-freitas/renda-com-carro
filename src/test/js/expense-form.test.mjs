import test from 'node:test';
import assert from 'node:assert/strict';

import {
  AllocationPreviewCoordinator,
  buildAllocationPreviewUrl,
  formatReferenceMonth,
  setExpenseSubmitState
} from '../../main/resources/static/js/expense-form.js';

test('expense form: reference month uses capitalized Brazilian presentation', () => {
  assert.equal(formatReferenceMonth('2026-07'), 'Julho de 2026');
  assert.equal(formatReferenceMonth(''), '—');
});

test('expense form: allocation preview URL carries vehicle and reference month', () => {
  assert.equal(
    buildAllocationPreviewUrl('abc-123', '2026-07'),
    '/expenses/allocation-preview?vehicleId=abc-123&competenceMonth=2026-07'
  );
});

test('expense form: a newer allocation request aborts and invalidates the previous one', () => {
  const coordinator = new AllocationPreviewCoordinator();
  const first = coordinator.begin();
  const second = coordinator.begin();

  assert.equal(first.signal.aborted, true);
  assert.equal(coordinator.isCurrent(first), false);
  assert.equal(coordinator.isCurrent(second), true);
});

test('expense form: submit state prevents duplicate submission and is reversible', () => {
  const button = { disabled: false, textContent: 'Salvar gasto', dataset: {} };
  const form = { attributes: new Map(), setAttribute(name, value) { this.attributes.set(name, value); }, removeAttribute(name) { this.attributes.delete(name); } };

  setExpenseSubmitState(form, button, true);
  assert.equal(button.disabled, true);
  assert.equal(button.textContent, 'Salvando…');
  assert.equal(form.attributes.get('aria-busy'), 'true');

  setExpenseSubmitState(form, button, false);
  assert.equal(button.disabled, false);
  assert.equal(button.textContent, 'Salvar gasto');
  assert.equal(form.attributes.has('aria-busy'), false);
});
