import test from 'node:test';
import assert from 'node:assert/strict';
import {
  activeObligationFieldNames,
  filterObligationPayload,
} from '../../main/resources/static/js/obligation-form-state.js';

test('known installment payload keeps only its active fixed fields', () => {
  const state = {
    mode: 'FIXED_INSTALLMENTS',
    calculationMethod: 'INSTALLMENT_KNOWN',
  };
  const payload = filterObligationPayload({
    creditor: 'Banco',
    principalAmount: '35000',
    firstDueDate: '2026-08-10',
    termMonths: '36',
    installmentAmount: '1386',
    interestRatePercent: '99',
    interestRatePeriod: 'MONTHLY',
    monthlyTarget: '500',
    singlePaymentAmount: '40000',
  }, state);

  assert.deepEqual(payload, {
    creditor: 'Banco',
    principalAmount: '35000',
    firstDueDate: '2026-08-10',
    termMonths: '36',
    installmentAmount: '1386',
  });
});

test('flexible known rate keeps target and rate fields without schedule fields', () => {
  const fields = activeObligationFieldNames({
    mode: 'FLEXIBLE_PAYMENTS',
    calculationMethod: 'RATE_KNOWN',
  });

  assert.ok(fields.has('monthlyTarget'));
  assert.ok(fields.has('interestRatePercent'));
  assert.ok(fields.has('interestRatePeriod'));
  assert.equal(fields.has('termMonths'), false);
  assert.equal(fields.has('installmentAmount'), false);
});

test('single payment keeps due date and total amount', () => {
  const fields = activeObligationFieldNames({ mode: 'SINGLE_PAYMENT' });
  assert.ok(fields.has('firstDueDate'));
  assert.ok(fields.has('singlePaymentAmount'));
  assert.equal(fields.has('monthlyTarget'), false);
});
