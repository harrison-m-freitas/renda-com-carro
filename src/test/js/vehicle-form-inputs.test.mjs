import test from 'node:test';
import assert from 'node:assert/strict';

import {
  formatMoneyInput,
  formatOdometerInput,
  formatVehiclePlate,
  normalizeVehicleText
} from '../../main/resources/static/js/vehicle-form-inputs.js';

test('vehicle input formatters: money uses card-terminal semantics', () => {
  assert.equal(formatMoneyInput(''), '');
  assert.equal(formatMoneyInput('1'), '0,01');
  assert.equal(formatMoneyInput('12'), '0,12');
  assert.equal(formatMoneyInput('123'), '1,23');
  assert.equal(formatMoneyInput('2399000'), '23.990,00');
});

test('vehicle input formatters: money accepts formatted paste and respects database precision', () => {
  assert.equal(formatMoneyInput('R$ 23.990,00'), '23.990,00');
  assert.equal(formatMoneyInput(' 23 990 00 '), '23.990,00');
  assert.equal(formatMoneyInput('123456789012345', 14), '123.456.789.012,34');
});

test('vehicle input formatters: odometer groups thousands and keeps one optional decimal', () => {
  assert.equal(formatOdometerInput('248351'), '248.351');
  assert.equal(formatOdometerInput('248351,5'), '248.351,5');
  assert.equal(formatOdometerInput('248351.5'), '248.351,5');
  assert.equal(formatOdometerInput('248.351,56'), '248.351,5');
});

test('vehicle input formatters: odometer preserves an in-progress comma and removes artificial zero on final formatting', () => {
  assert.equal(formatOdometerInput('248351,'), '248.351,');
  assert.equal(formatOdometerInput('0,0'), '0,0');
  assert.equal(formatOdometerInput('0,0', { trimZeroFraction: true }), '0');
  assert.equal(formatOdometerInput('248351,0', { trimZeroFraction: true }), '248.351');
});

test('vehicle input formatters: plate supports legacy, Mercosul and partial input', () => {
  assert.equal(formatVehiclePlate('abc1234'), 'ABC-1234');
  assert.equal(formatVehiclePlate('abc-1234'), 'ABC-1234');
  assert.equal(formatVehiclePlate('abc1d23'), 'ABC1D23');
  assert.equal(formatVehiclePlate('ab c-1'), 'ABC-1');
  assert.equal(formatVehiclePlate('abc1d2345'), 'ABC1D23');
});

test('vehicle input formatters: text normalization preserves capitalization', () => {
  assert.equal(normalizeVehicleText('  Land   Rover  '), 'Land Rover');
  assert.equal(normalizeVehicleText(' BMW '), 'BMW');
  assert.equal(normalizeVehicleText('  CR-V  '), 'CR-V');
  assert.equal(normalizeVehicleText('   '), '');
});
