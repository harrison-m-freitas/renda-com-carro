import test from 'node:test';
import assert from 'node:assert/strict';
import { readdirSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const migrationDirectory = resolve('src/main/resources/db/migration');

test('finance redesign follows merged Flyway versions and removes incompatible obligation drafts', () => {
  const files = readdirSync(migrationDirectory);
  const versions = files
    .map((name) => /^(V\d+)__/.exec(name)?.[1])
    .filter(Boolean);

  assert.equal(new Set(versions).size, versions.length, 'Flyway versions must be unique');
  assert.ok(files.includes('V11__add_user_time_zone.sql'));
  assert.ok(files.includes('V12__enforce_single_obligation_draft.sql'));
  assert.ok(files.includes('V13__add_monthly_goal_vehicles_and_shared_expenses.sql'));
  assert.ok(files.includes('V14__track_installment_payments.sql'));
  assert.ok(files.includes('V15__enforce_single_active_vehicle_and_goal_vehicle.sql'));
  assert.ok(files.includes('V16__redesign_financial_obligations.sql'));
  assert.equal(files.includes('V13__redesign_financial_obligations.sql'), false);
  assert.equal(files.includes('V11__redesign_financial_obligations.sql'), false);

  const financeMigration = readFileSync(
    resolve(migrationDirectory, 'V16__redesign_financial_obligations.sql'),
    'utf8',
  );
  assert.match(financeMigration, /DELETE FROM form_draft WHERE form_type = 'OBLIGATION'/);
});
