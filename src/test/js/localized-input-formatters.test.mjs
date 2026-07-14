import test from 'node:test';
import assert from 'node:assert/strict';

import {
  applyFixedScaleEdit,
  formatMoneyInput,
  formatPercentageInput,
  formatOdometerInput,
  parseLocalizedDecimal,
  normalizeSpaces
} from '../../main/resources/static/js/localized-input-formatters.js';

test('localized formatters: money uses card-terminal semantics', () => {
  assert.equal(formatMoneyInput(''), '');
  assert.equal(formatMoneyInput('1'), '0,01');
  assert.equal(formatMoneyInput('12'), '0,12');
  assert.equal(formatMoneyInput('125'), '1,25');
  assert.equal(formatMoneyInput('2399000'), '23.990,00');
  assert.equal(formatMoneyInput('R$ 23.990,00'), '23.990,00');
  assert.equal(formatMoneyInput('123456789012345', 14), '123.456.789.012,34');
});

test('localized formatters: percentage uses two implicit decimals without clamping', () => {
  assert.equal(formatPercentageInput('1'), '0,01');
  assert.equal(formatPercentageInput('12'), '0,12');
  assert.equal(formatPercentageInput('125'), '1,25');
  assert.equal(formatPercentageInput('1250'), '12,50');
  assert.equal(formatPercentageInput('10000'), '100,00');
  assert.equal(formatPercentageInput('10001'), '100,01');
});

test('localized formatters: fixed-scale edits support typing replacement and deletion', () => {
  assert.equal(applyFixedScaleEdit('', { inputType: 'insertText', data: '1' }), '1');
  assert.equal(applyFixedScaleEdit('12', { inputType: 'insertText', data: '3' }), '123');
  assert.equal(applyFixedScaleEdit('123', { inputType: 'deleteContentBackward' }), '12');
  assert.equal(applyFixedScaleEdit('123', {
    inputType: 'deleteContentBackward',
    replaceAll: true
  }), '');
  assert.equal(applyFixedScaleEdit('123', {
    inputType: 'insertFromPaste',
    data: 'R$ 23.990,00',
    replaceAll: true
  }), '2399000');
});

test('localized formatters: fixed-scale edits replace a partial digit selection', () => {
  assert.equal(applyFixedScaleEdit('12345', {
    inputType: 'insertText',
    data: '9',
    selectionStart: 1,
    selectionEnd: 4
  }), '195');
  assert.equal(applyFixedScaleEdit('12345', {
    inputType: 'deleteContentBackward',
    selectionStart: 1,
    selectionEnd: 4
  }), '15');
});

test('localized formatters: odometer groups thousands and keeps one optional decimal', () => {
  assert.equal(formatOdometerInput('248351'), '248.351');
  assert.equal(formatOdometerInput('248351,5'), '248.351,5');
  assert.equal(formatOdometerInput('248351.5'), '248.351,5');
  assert.equal(formatOdometerInput('248.351,56'), '248.351,5');
  assert.equal(formatOdometerInput('248351,'), '248.351,');
  assert.equal(formatOdometerInput('248351,0', { trimZeroFraction: true }), '248.351');
});

test('localized formatters: parser accepts Brazilian values', () => {
  assert.equal(parseLocalizedDecimal('23.990,00'), 23990);
  assert.equal(parseLocalizedDecimal('1,25'), 1.25);
  assert.equal(parseLocalizedDecimal('100'), 100);
  assert.equal(parseLocalizedDecimal(''), null);
  assert.equal(parseLocalizedDecimal('não numérico'), null);
});

test('localized formatters: space normalization preserves capitalization', () => {
  assert.equal(normalizeSpaces('  Banco   Familiar  '), 'Banco Familiar');
  assert.equal(normalizeSpaces(' BMW '), 'BMW');
  assert.equal(normalizeSpaces('   '), '');
});
