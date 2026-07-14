import test from 'node:test';
import assert from 'node:assert/strict';

import {
  civilDateInTimeZone,
  decisionKey,
  shouldPromptForTimeZone
} from '../../main/resources/static/js/user-time-zone.js';

test('same saved and detected zone does not prompt', () => {
  assert.equal(
    shouldPromptForTimeZone('America/Sao_Paulo', 'America/Sao_Paulo', new Map()),
    false
  );
});

test('declined saved and detected pair is remembered only for that pair', () => {
  const storage = new Map([
    [decisionKey('America/Sao_Paulo', 'America/New_York'), 'keep']
  ]);

  assert.equal(
    shouldPromptForTimeZone('America/Sao_Paulo', 'America/New_York', storage),
    false
  );
  assert.equal(
    shouldPromptForTimeZone('America/Sao_Paulo', 'Europe/Lisbon', storage),
    true
  );
});

test('civil date uses the requested IANA zone without UTC slicing', () => {
  const instant = new Date('2026-07-14T10:30:00Z');

  assert.equal(civilDateInTimeZone(instant, 'Pacific/Kiritimati'), '2026-07-15');
  assert.equal(civilDateInTimeZone(instant, 'America/Sao_Paulo'), '2026-07-14');
});
