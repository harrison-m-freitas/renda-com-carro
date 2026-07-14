import test from 'node:test';
import assert from 'node:assert/strict';

import { formatVehiclePlate } from '../../main/resources/static/js/vehicle-form-inputs.js';

test('vehicle input formatter: plate supports legacy Mercosul and partial input', () => {
  assert.equal(formatVehiclePlate('abc1234'), 'ABC-1234');
  assert.equal(formatVehiclePlate('abc-1234'), 'ABC-1234');
  assert.equal(formatVehiclePlate('abc1d23'), 'ABC1D23');
  assert.equal(formatVehiclePlate('ab c-1'), 'ABC-1');
  assert.equal(formatVehiclePlate('abc1d2345'), 'ABC1D23');
});
