import test from 'node:test';
import assert from 'node:assert/strict';

import {
  formatVehiclePlate,
  formatVehiclePlateEdit
} from '../../main/resources/static/js/vehicle-form-inputs.js';

test('vehicle input formatter: plate supports legacy Mercosul and partial input', () => {
  assert.equal(formatVehiclePlate('abc1234'), 'ABC-1234');
  assert.equal(formatVehiclePlate('abc-1234'), 'ABC-1234');
  assert.equal(formatVehiclePlate('abc1d23'), 'ABC1D23');
  assert.equal(formatVehiclePlate('ab c-1'), 'ABC-1');
  assert.equal(formatVehiclePlate('abc1d2345'), 'ABC1D23');
});

test('vehicle input formatter: edit preserves a caret around the legacy hyphen', () => {
  assert.deepEqual(formatVehiclePlateEdit('abc1234', 3, 3), {
    value: 'ABC-1234',
    selectionStart: 3,
    selectionEnd: 3
  });
  assert.deepEqual(formatVehiclePlateEdit('abc1234', 4, 4), {
    value: 'ABC-1234',
    selectionStart: 5,
    selectionEnd: 5
  });
});

test('vehicle input formatter: edit preserves a non-collapsed selection', () => {
  assert.deepEqual(formatVehiclePlateEdit('ab-c1234', 1, 6), {
    value: 'ABC-1234',
    selectionStart: 1,
    selectionEnd: 6
  });
});

test('vehicle input formatter: Mercosul edit does not invent a hyphen', () => {
  assert.deepEqual(formatVehiclePlateEdit('abc1d23', 4, 4), {
    value: 'ABC1D23',
    selectionStart: 4,
    selectionEnd: 4
  });
});

test('vehicle input formatter: offsets are clamped after invalid characters and truncation', () => {
  assert.deepEqual(formatVehiclePlateEdit('@@abc1d2345', 11, 11), {
    value: 'ABC1D23',
    selectionStart: 7,
    selectionEnd: 7
  });
});

test('vehicle input formatter: edit formatting is idempotent', () => {
  const first = formatVehiclePlateEdit('abc1234', 7, 7);
  assert.deepEqual(formatVehiclePlateEdit(
    first.value,
    first.selectionStart,
    first.selectionEnd
  ), first);
});
