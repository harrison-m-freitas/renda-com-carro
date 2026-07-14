import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const cssUrl = new URL('../../main/resources/static/css/app.css', import.meta.url);
const loadCss = () => readFile(cssUrl, 'utf8');

test('vehicle styles: declare responsive flow progress disclosure and hidden states', async () => {
  const css = await loadCss();

  for (const selector of [
    '.vehicle-form-progress__copy',
    '.vehicle-form-progress__track',
    '.vehicle-acquisition-toggle',
    '.vehicle-form--enhanced [data-vehicle-step][hidden]',
    '.vehicle-form--enhanced [data-vehicle-acquisition-panel][hidden]',
    '[data-vehicle-previous]',
    '[data-vehicle-next]'
  ]) {
    assert.ok(css.includes(selector), `missing ${selector}`);
  }
});

test('vehicle styles: disclosure and actions preserve touch target and safe area', async () => {
  const css = await loadCss();

  assert.match(css, /\.vehicle-acquisition-toggle\s*\{[^}]*min-height:\s*44px/s);
  assert.match(css, /env\(safe-area-inset-bottom\)/);
  assert.match(css, /@media \(min-width:\s*768px\)/);
  assert.match(css, /@media \(prefers-reduced-motion:\s*reduce\)/);
});
