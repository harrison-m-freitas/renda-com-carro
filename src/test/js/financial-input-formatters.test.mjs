import test from 'node:test';
import assert from 'node:assert/strict';
import {
  formatFinancialMoney,
  formatFinancialPercentage,
  formatCanonicalMoney,
  formatCanonicalPercentage,
  parseCanonicalFinancialDecimal,
  parseFinancialDecimal,
  sanitizeFinancialEntry,
} from '../../main/resources/static/js/financial-input-formatters.js';

test('financial formatter treats 1000 as one thousand instead of ten reais', () => {
  assert.equal(formatFinancialMoney('1000'), '1.000,00');
  assert.equal(parseFinancialDecimal('1.000,00'), 1000);
});

test('financial formatter accepts partial decimals and pasted currency', () => {
  assert.equal(formatFinancialMoney('1000,5'), '1.000,50');
  assert.equal(formatFinancialMoney('R$ 1.234,56'), '1.234,56');
  assert.equal(parseFinancialDecimal('R$ 1.234,56'), 1234.56);
});

test('financial percentage keeps natural percentage semantics', () => {
  assert.equal(formatFinancialPercentage('12'), '12,00');
  assert.equal(formatFinancialPercentage('2,06'), '2,06');
  assert.equal(parseFinancialDecimal('2,06'), 2.06);
});

test('entry sanitization preserves one decimal separator while typing', () => {
  assert.equal(sanitizeFinancialEntry('R$ 1.000,5x'), '1000,5');
  assert.equal(sanitizeFinancialEntry('12.34.56'), '12,3456');
  assert.equal(sanitizeFinancialEntry(''), '');
});


test('canonical percentage preserves three decimal places', () => {
  assert.equal(parseCanonicalFinancialDecimal('0.005'), 0.005);
  assert.equal(formatCanonicalPercentage('0.005'), '0,005');
});

test('canonical money restores server decimals without grouping ambiguity', () => {
  assert.equal(formatCanonicalMoney('35000.00'), '35.000,00');
  assert.equal(formatCanonicalMoney('1000.000'), '1.000,00');
});
