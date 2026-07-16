import test from 'node:test';
import assert from 'node:assert/strict';
import {
  buildObligationPreview,
  inferMonthlyRate,
  suggestFirstDueDate,
} from '../../main/resources/static/js/obligation-calculator.js';

test('browser preview infers the bank rate from 36 installments of 1386', () => {
  const preview = buildObligationPreview({
    mode: 'FIXED_INSTALLMENTS',
    calculationMethod: 'INSTALLMENT_KNOWN',
    principalAmount: 35000,
    installmentAmount: 1386,
    termMonths: 36,
    firstDueDate: '2026-08-10',
  });

  assert.equal(preview.valid, true);
  assert.ok(preview.monthlyRatePercent > 2.05 && preview.monthlyRatePercent < 2.08);
  assert.ok(preview.annualRatePercent > 27.5 && preview.annualRatePercent < 28);
  assert.equal(preview.installmentAmount, 1386);
  assert.equal(preview.lastDueDate, '2029-07-10');
  assert.ok(preview.totalAmount > 49000);
});

test('browser preview calculates installment from a known effective annual rate', () => {
  const preview = buildObligationPreview({
    mode: 'FIXED_INSTALLMENTS',
    calculationMethod: 'RATE_KNOWN',
    principalAmount: 12000,
    interestRatePercent: 12,
    interestRatePeriod: 'ANNUAL',
    termMonths: 12,
    firstDueDate: '2026-08-10',
  });

  assert.equal(preview.valid, true);
  assert.ok(preview.installmentAmount > 1000);
  assert.ok(Math.abs(preview.annualRatePercent - 12) < 0.001);
});

test('browser preview rejects an installment that cannot amortize the debt', () => {
  const preview = buildObligationPreview({
    mode: 'FIXED_INSTALLMENTS',
    calculationMethod: 'INSTALLMENT_KNOWN',
    principalAmount: 10000,
    installmentAmount: 800,
    termMonths: 12,
    firstDueDate: '2026-08-10',
  });

  assert.equal(preview.valid, false);
  assert.equal(preview.errorField, 'installmentAmount');
});

test('flexible preview warns when target is below first-month interest', () => {
  const preview = buildObligationPreview({
    mode: 'FLEXIBLE_PAYMENTS',
    calculationMethod: 'RATE_KNOWN',
    principalAmount: 10000,
    monthlyTarget: 100,
    interestRatePercent: 2,
    interestRatePeriod: 'MONTHLY',
  });

  assert.equal(preview.valid, true);
  assert.equal(preview.amortizes, false);
  assert.match(preview.warning, /não é suficiente/i);
});

test('rate solver returns zero at the interest-free installment', () => {
  assert.equal(inferMonthlyRate(12000, 1000, 12), 0);
});


test('first due date suggestion clamps month-end dates instead of skipping a month', () => {
  assert.equal(suggestFirstDueDate('2026-01-31'), '2026-02-28');
  assert.equal(suggestFirstDueDate('2028-01-31'), '2028-02-29');
});

test('interest-free preview distributes cents without a zero final installment', () => {
  const preview = buildObligationPreview({
    mode: 'FIXED_INSTALLMENTS',
    calculationMethod: 'INTEREST_FREE',
    principalAmount: 1,
    termMonths: 18,
    firstDueDate: '2026-08-10',
  });

  assert.equal(preview.valid, true);
  assert.equal(preview.totalAmount, 1);
  assert.equal(preview.installmentAmount, 0.06);
  assert.deepEqual(preview.installmentAmounts, { higher: 0.06, higherCount: 10, lower: 0.05, lowerCount: 8 });
});

test('interest-free preview keeps the rounded average as the displayed installment', () => {
  const preview = buildObligationPreview({
    mode: 'FIXED_INSTALLMENTS',
    calculationMethod: 'INTEREST_FREE',
    principalAmount: 10000,
    termMonths: 3,
    firstDueDate: '2026-08-10',
  });

  assert.equal(preview.valid, true);
  assert.equal(preview.installmentAmount, 3333.33);
  assert.deepEqual(preview.installmentAmounts, {
    higher: 3333.34,
    higherCount: 1,
    lower: 3333.33,
    lowerCount: 2,
  });
});
