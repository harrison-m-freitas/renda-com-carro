# Compact Expense Step 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the oversized card-heavy layout of expense step 2 with compact segmented radio groups and a lightweight progressive mixed-allocation panel inspired by Shadcn's density and hierarchy.

**Architecture:** Preserve all existing field names, radio semantics, backend contracts, draft schema, state reducer, allocation preview endpoint, and validation rules. Change only template composition, small presentation helpers in `expense-form.js`, CSS, and contract tests. The browser will continue using native radios inside `fieldset`/`legend`; JavaScript only updates the single contextual description for each active group.

**Tech Stack:** Thymeleaf, Bootstrap/Tabler-compatible CSS, JavaScript ES modules, Node test runner, Spring Boot MockMvc contracts.

## Global Constraints

- Work only on `feat/expense-form-ux-accessibility`; do not merge automatically.
- Do not wait for CI between tasks; run local JavaScript checks while implementing and wait for complete CI only after the final commit.
- Do not change database, entities, domain calculations, allocation endpoint, draft schema, or submission service.
- Keep native radios, `fieldset`, `legend`, keyboard arrow navigation, visible focus, and error-summary anchors.
- Keep mobile touch targets at least 40 px and prevent horizontal overflow at 320 px.
- Use subtle selected backgrounds, 1 px borders, 6–8 px radii, and no double selection shadow.

---

### Task 1: Add presentation contracts for compact groups

**Files:**
- Modify: `src/test/js/expense-form.test.mjs`
- Create: `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseCompactLayoutWebTest.java`

**Interfaces:**
- Produces: `classificationDescription(value)` and `paymentDescription(value)` from `expense-form.js`.
- Requires stable markup hooks: `data-classification-description`, `data-payment-description`, `expense-segmented-group`, `expense-allocation-methods`, and `expense-reference-control`.

- [ ] **Step 1: Add failing JavaScript tests**

```javascript
import {
  classificationDescription,
  paymentDescription
} from '../../main/resources/static/js/expense-form.js';

test('expense form: classification help only describes the active option', () => {
  assert.equal(classificationDescription('PROFESSIONAL'), 'Todo o valor será considerado custo da operação.');
  assert.equal(classificationDescription('PERSONAL'), 'O valor não reduzirá o resultado profissional.');
  assert.equal(classificationDescription('MIXED'), 'Uma parte será profissional e a outra pessoal.');
});

test('expense form: payment help only describes the active option', () => {
  assert.equal(paymentDescription('PAID'), 'O pagamento já foi realizado.');
  assert.equal(paymentDescription('PENDING'), 'O gasto ainda será pago.');
});
```

- [ ] **Step 2: Run the focused JavaScript test and verify RED**

Run: `node --test src/test/js/expense-form.test.mjs`
Expected: FAIL because the two helpers are not exported.

- [ ] **Step 3: Add a failing MockMvc contract test**

The test must GET `/expenses/new` as an authenticated owner and assert the response contains:

```text
expense-segmented-group
expense-allocation-methods
data-classification-description
data-payment-description
expense-reference-control
```

It must also assert the old visual classes are absent from the form markup:

```text
classification-card-grid
classification-card__content
payment-status-choice
allocation-choice-list
```

- [ ] **Step 4: Do not wait for CI; continue after confirming the JavaScript RED locally**

### Task 2: Recompose the Thymeleaf markup

**Files:**
- Modify: `src/main/resources/templates/expenses/form.html`

**Interfaces:**
- Consumes unchanged field names: `classification`, `allocationMethod`, `paymentStatus`, `paidDate`, and `competenceMonth`.
- Produces stable contextual help nodes consumed by `expense-form.js`.

- [ ] **Step 1: Replace classification cards with a compact segmented group**

Use this semantic structure:

```html
<fieldset class="expense-choice-section" id="classification" tabindex="-1"
          aria-describedby="classification-description classification-error">
  <legend class="form-label mb-2">Classificação do gasto <span class="required-mark" aria-hidden="true">*</span></legend>
  <div class="expense-segmented-group expense-segmented-group--three">
    <label class="expense-segmented-option">
      <input class="form-check-input" type="radio" th:field="*{classification}" value="PROFESSIONAL" required>
      <span>Profissional</span>
    </label>
    <label class="expense-segmented-option">
      <input class="form-check-input" type="radio" th:field="*{classification}" value="PERSONAL" required>
      <span>Pessoal</span>
    </label>
    <label class="expense-segmented-option">
      <input class="form-check-input" type="radio" th:field="*{classification}" value="MIXED" required>
      <span>Misto</span>
    </label>
  </div>
  <p class="form-text expense-choice-description" id="classification-description"
     data-classification-description></p>
  <div class="invalid-feedback d-block" id="classification-error" th:errors="*{classification}"></div>
</fieldset>
```

- [ ] **Step 2: Replace the mixed-allocation card stack with a compact subpanel**

Use short labels `Quilometragem`, `Percentual`, and `Valor`; retain the same radio values and `data-allocation-group`. Put the active-method explanatory sentence in one `data-allocation-method-description` paragraph. Keep percentage, amount, reason, and preview nodes unchanged except for their layout wrappers.

- [ ] **Step 3: Replace payment cards with a compact binary segmented group**

Keep `paymentStatus` radios and add a single paragraph with `data-payment-description`.

- [ ] **Step 4: Make reference a secondary inline control**

Keep `<details>` and `<summary>`, but use the class `expense-reference-control`. The always-visible summary must read `Referência: <strong data-reference-label>…</strong>` followed by a small `Alterar` action. The month input remains inside the opened details.

### Task 3: Add contextual presentation helpers

**Files:**
- Modify: `src/main/resources/static/js/expense-form.js`
- Modify: `src/test/js/expense-form.test.mjs`

**Interfaces:**
- Produces:
  - `classificationDescription(value: string): string`
  - `paymentDescription(value: string): string`
  - `allocationMethodDescription(value: string): string`

- [ ] **Step 1: Implement the minimal pure helpers**

```javascript
export function classificationDescription(value) {
  return {
    PROFESSIONAL: 'Todo o valor será considerado custo da operação.',
    PERSONAL: 'O valor não reduzirá o resultado profissional.',
    MIXED: 'Uma parte será profissional e a outra pessoal.'
  }[value] ?? '';
}

export function paymentDescription(value) {
  return {
    PAID: 'O pagamento já foi realizado.',
    PENDING: 'O gasto ainda será pago.'
  }[value] ?? '';
}

export function allocationMethodDescription(value) {
  return {
    MILEAGE_RATIO: 'Usa o fechamento confirmado ou a estimativa disponível para o mês.',
    MANUAL_PERCENTAGE: 'Informe quanto do gasto pertence à operação em percentual.',
    FIXED_AMOUNT: 'Informe diretamente o valor profissional do gasto.'
  }[value] ?? '';
}
```

- [ ] **Step 2: In `initializeExpenseForm`, query the three description nodes**

Add:

```javascript
const classificationDescriptionElement = form.querySelector('[data-classification-description]');
const paymentDescriptionElement = form.querySelector('[data-payment-description]');
const allocationMethodDescriptionElement = form.querySelector('[data-allocation-method-description]');
```

- [ ] **Step 3: Update them in `render()`**

```javascript
setText(classificationDescriptionElement, classificationDescription(state.classification));
setText(paymentDescriptionElement, paymentDescription(state.paymentStatus));
setText(allocationMethodDescriptionElement, allocationMethodDescription(state.allocationMethod));
```

- [ ] **Step 4: Run focused tests and verify GREEN**

Run: `node --test src/test/js/expense-form.test.mjs`
Expected: all tests in the file pass.

### Task 4: Replace heavy card CSS with compact Shadcn-inspired styling

**Files:**
- Modify: `src/main/resources/static/css/app.css`

**Interfaces:**
- Styles the markup produced by Task 2 without changing global Bootstrap controls.

- [ ] **Step 1: Remove obsolete expense-only card selectors**

Remove or stop using:

```text
.classification-card-grid
.classification-card
.classification-card__content
.allocation-choice-list
.allocation-choice
.payment-status-group
.payment-status-choice
```

- [ ] **Step 2: Add compact segmented groups**

Implement:

```css
.expense-choice-section { margin: 0 0 1.25rem; }
.expense-segmented-group {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: .5rem;
}
.expense-segmented-group--three { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.expense-segmented-option {
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: .5rem;
  padding: .5rem .75rem;
  margin: 0;
  border: 1px solid var(--bs-border-color);
  border-radius: .5rem;
  background: var(--bs-body-bg);
  cursor: pointer;
  font-size: .875rem;
  font-weight: 600;
  transition: border-color .15s ease, background-color .15s ease, color .15s ease;
}
.expense-segmented-option:has(input:checked) {
  border-color: var(--bs-primary);
  background: rgba(var(--bs-primary-rgb), .075);
  color: var(--bs-primary-text-emphasis);
}
.expense-segmented-option:focus-within {
  outline: 3px solid rgba(var(--bs-primary-rgb), .2);
  outline-offset: 2px;
}
.expense-segmented-option .form-check-input { margin: 0; flex: 0 0 auto; }
.expense-choice-description { min-height: 1.25rem; margin-top: .5rem; }
```

- [ ] **Step 3: Style the mixed panel and preview as subordinate content**

Use a neutral `var(--bs-tertiary-bg)` background, 1 px border, 8 px radius, 12–16 px padding, and compact internal spacing. The preview must use a very subtle info background and no full-width cyan block.

- [ ] **Step 4: Style the reference details as secondary inline information**

The closed summary must not look like a full input. It should have no fixed input height, use a transparent background, and expose a subtle `Alterar` affordance.

- [ ] **Step 5: Add responsive rules**

At widths below 576 px, allow segmented groups to remain compact; the three-option group may use three columns if labels fit, otherwise switch to one column only below approximately 360 px. The allocation method group may become one column below 576 px. No horizontal overflow.

### Task 5: Local verification, final commit, then complete CI

**Files:**
- Verify all files changed above.

- [ ] **Step 1: Run the complete JavaScript suite locally**

Run: `node --test src/test/js/*.test.mjs`
Expected: 0 failures.

- [ ] **Step 2: Perform static checks**

Confirm:

```bash
! grep -q 'classification-card-grid' src/main/resources/templates/expenses/form.html
! grep -q 'payment-status-choice' src/main/resources/templates/expenses/form.html
grep -q 'expense-segmented-group' src/main/resources/templates/expenses/form.html
grep -q 'data-classification-description' src/main/resources/templates/expenses/form.html
```

- [ ] **Step 3: Commit all implementation changes**

Use commit messages that separate tests, markup/JS, and CSS when practical, but do not pause for CI between them.

- [ ] **Step 4: Wait for the final PR CI only after the full implementation is pushed**

Validate JavaScript, Java/MockMvc/Testcontainers, Maven package, credentials, ARM64 image, stack startup, healthcheck, backup, and isolated restore.

- [ ] **Step 5: If CI fails, inspect the exact failing job and correct the root cause on the same branch**

Do not merge automatically.