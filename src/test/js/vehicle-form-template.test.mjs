import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const templateUrl = new URL(
  '../../main/resources/templates/vehicles/form.html',
  import.meta.url
);

const loadTemplate = () => readFile(templateUrl, 'utf8');

test('vehicle template: declares standalone two-step mobile flow', async () => {
  const html = await loadTemplate();

  for (const fragment of [
    'data-vehicle-flow',
    'data-vehicle-progress',
    'data-vehicle-step="identification"',
    'data-vehicle-step="operation"',
    'data-vehicle-next',
    'data-vehicle-previous',
    'data-vehicle-cancel-action',
    'data-vehicle-acquisition-toggle',
    'data-vehicle-acquisition-panel',
    'data-vehicle-submit-spinner',
    'data-vehicle-submit-copy'
  ]) {
    assert.ok(html.includes(fragment), `missing ${fragment}`);
  }

  assert.equal(html.includes('data-guided-form'), false);
  assert.equal(html.includes('data-draft-type'), false);
});

test('vehicle template: places essential identification fields before optional name', async () => {
  const html = await loadTemplate();
  const positions = ['id="make"', 'id="model"', 'id="year"', 'id="plate"', 'id="name"']
    .map((fragment) => html.indexOf(fragment));

  assert.ok(positions.every((position) => position >= 0));
  assert.deepEqual([...positions].sort((left, right) => left - right), positions);
  assert.ok(html.includes('Nome para identificação'));
});
