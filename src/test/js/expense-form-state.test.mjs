import test from 'node:test';
import assert from 'node:assert/strict';

import {
  createExpenseState,
  expenseDraftPayload,
  expenseSummary,
  formatMoneyFromCents,
  updateExpenseState,
  validateExpenseState
} from '../../main/resources/static/js/expense-form-state.js';

const mixedPercentage = (value) => createExpenseState({
  amount: '100,00',
  classification: 'MIXED',
  allocationMethod: 'MANUAL_PERCENTAGE',
  professionalPercentagePercent: value,
  adjustmentReason: 'Uso profissional comprovado',
  paymentStatus: 'PAID',
  paidDate: '2026-07-14'
});

test('professional and personal classifications allocate the complete total safely', () => {
  const professional = expenseSummary(createExpenseState({
    amount: '123,45', classification: 'PROFESSIONAL'
  }));
  const personal = expenseSummary(createExpenseState({
    amount: '123,45', classification: 'PERSONAL'
  }));

  assert.equal(formatMoneyFromCents(professional.professionalCents), 'R$ 123,45');
  assert.equal(formatMoneyFromCents(professional.personalCents), 'R$ 0,00');
  assert.equal(formatMoneyFromCents(personal.professionalCents), 'R$ 0,00');
  assert.equal(formatMoneyFromCents(personal.personalCents), 'R$ 123,45');
});

test('mixed percentage rejects zero and one hundred', () => {
  assert.equal(
    validateExpenseState(mixedPercentage('0')).professionalPercentagePercent,
    'Para 0%, classifique o gasto como Pessoal.'
  );
  assert.equal(
    validateExpenseState(mixedPercentage('100')).professionalPercentagePercent,
    'Para 100%, classifique o gasto como Profissional.'
  );
});

test('mixed fixed amount must stay strictly between zero and total', () => {
  const base = {
    amount: '100,00',
    classification: 'MIXED',
    allocationMethod: 'FIXED_AMOUNT',
    adjustmentReason: 'Divisão comprovada'
  };
  assert.equal(
    validateExpenseState(createExpenseState({ ...base, professionalFixedAmount: '0,00' })).professionalFixedAmount,
    'Para nenhum valor profissional, classifique o gasto como Pessoal.'
  );
  assert.equal(
    validateExpenseState(createExpenseState({ ...base, professionalFixedAmount: '100,00' })).professionalFixedAmount,
    'Para atribuir todo o valor à operação, classifique o gasto como Profissional.'
  );
  assert.equal(
    validateExpenseState(createExpenseState({ ...base, professionalFixedAmount: '101,00' })).professionalFixedAmount,
    'O valor profissional deve ser menor que o valor total do gasto.'
  );
});

test('paid date and reference month follow expense date until manually edited', () => {
  let state = createExpenseState({ expenseDate: '2026-07-14', paymentStatus: 'PAID' });
  state = updateExpenseState(state, { type: 'EXPENSE_DATE_CHANGED', value: '2026-08-01' });
  assert.equal(state.paidDate, '2026-08-01');
  assert.equal(state.competenceMonth, '2026-08');

  state = updateExpenseState(state, { type: 'PAID_DATE_CHANGED', value: '2026-07-30' });
  state = updateExpenseState(state, { type: 'COMPETENCE_MONTH_CHANGED', value: '2026-09' });
  state = updateExpenseState(state, { type: 'EXPENSE_DATE_CHANGED', value: '2026-08-02' });
  assert.equal(state.paidDate, '2026-07-30');
  assert.equal(state.competenceMonth, '2026-09');
});

test('pending removes effective paid date but restores transient manual value', () => {
  let state = createExpenseState({
    expenseDate: '2026-07-14', paymentStatus: 'PAID', paidDate: '2026-07-10'
  });
  state = updateExpenseState(state, { type: 'PAYMENT_STATUS_CHANGED', value: 'PENDING' });
  assert.equal(expenseDraftPayload(state).paidDate, undefined);
  state = updateExpenseState(state, { type: 'PAYMENT_STATUS_CHANGED', value: 'PAID' });
  assert.equal(state.paidDate, '2026-07-10');
});

test('incompatible allocation fields are omitted without erasing transient state', () => {
  let state = createExpenseState({
    classification: 'MIXED',
    allocationMethod: 'MANUAL_PERCENTAGE',
    professionalPercentagePercent: '75',
    adjustmentReason: 'Uso profissional comprovado'
  });
  state = updateExpenseState(state, { type: 'CLASSIFICATION_CHANGED', value: 'PERSONAL' });
  const payload = expenseDraftPayload(state);

  assert.equal(payload.allocationMethod, undefined);
  assert.equal(payload.professionalPercentagePercent, undefined);
  assert.equal(state.professionalPercentagePercent, '75');
  assert.equal(state.adjustmentReason, 'Uso profissional comprovado');
});

test('confirmed and estimated mileage summaries use server percentages without persisting them', () => {
  const state = createExpenseState({
    amount: '100,00', classification: 'MIXED', allocationMethod: 'MILEAGE_RATIO'
  });
  const estimated = expenseSummary(state, {
    status: 'ESTIMATED', professionalPercentage: '69.3500', provisional: true
  });

  assert.equal(formatMoneyFromCents(estimated.professionalCents), 'R$ 69,35');
  assert.equal(formatMoneyFromCents(estimated.personalCents), 'R$ 30,65');
  assert.equal(estimated.provisional, true);
  assert.equal(expenseDraftPayload(state).professionalPercentage, undefined);
});

test('insufficient mileage data does not invent a split', () => {
  const summary = expenseSummary(createExpenseState({
    amount: '100,00', classification: 'MIXED', allocationMethod: 'MILEAGE_RATIO'
  }), { status: 'INSUFFICIENT_DATA' });

  assert.equal(summary.calculable, false);
  assert.equal(summary.professionalCents, null);
  assert.equal(summary.personalCents, null);
});
