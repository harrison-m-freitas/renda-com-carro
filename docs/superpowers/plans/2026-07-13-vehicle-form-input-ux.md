# Vehicle Form Input UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Melhorar o preenchimento do formulário de veículo com máscaras brasileiras previsíveis, placas antiga e Mercosul, limpeza de textos, validação acessível e campos com larguras proporcionais ao conteúdo.

**Architecture:** Separar as transformações puras de entrada em um pequeno módulo ES, mantendo o `vehicle-form.js` responsável apenas pela integração com o DOM, validação, estado de envio e navegação. O template Thymeleaf fornecerá atributos declarativos para cada comportamento e o CSS usará grades proporcionais com `minmax()` e `fr`, sem biblioteca externa de máscaras.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Thymeleaf, Bootstrap 5.3.7, JavaScript ES modules, Node.js 22 com `node:test`, JUnit 5, MockMvc e Testcontainers/PostgreSQL.

## Global Constraints

- Restringir a mudança ao formulário de criação e edição de veículo.
- Não adicionar biblioteca externa de máscaras.
- Não criar wizard, nova tela ou novo layout.
- Preservar a estrutura em seções **Identificação**, **Dados de operação** e **Aquisição**.
- Manter preço de compra opcional; vazio deve continuar vazio e persistir como `NULL`.
- Usar máscara monetária de maquininha: os dois últimos dígitos representam centavos.
- Exibir odômetro inteiro sem `,0`; aceitar no máximo uma casa decimal opcional.
- Aceitar e formatar placa antiga `ABC-1234` e Mercosul `ABC1D23`.
- Preservar capitalização de apelido, marca e modelo; apenas normalizar espaços.
- Usar proporções, `minmax()` e `fr` para larguras de campos; não atribuir larguras rígidas em pixels aos campos.
- Manter ano e placa lado a lado por padrão, quebrando somente em telas extremamente estreitas.
- Manter a barra de ações fixa somente no mobile.
- A máscara complementa, mas não substitui, validação HTML e validação do servidor.
- Trabalhar em TDD, executar o teste vermelho antes da implementação e fazer commits pequenos por tarefa.

---

## File Map

- Create: `src/main/resources/static/js/vehicle-form-inputs.js`
  - Funções puras para dinheiro, odômetro, placa e espaços.
- Create: `src/test/js/vehicle-form-inputs.test.mjs`
  - Testes unitários das transformações puras.
- Modify: `src/main/resources/static/js/vehicle-form.js:1-88`
  - Integração das funções puras com o formulário, validação, envio, histórico e aviso de alterações.
- Create: `src/test/js/vehicle-form.test.mjs`
  - Testes do estado de envio inválido e restauração por `pageshow` sem dependência de DOM externo.
- Modify: `src/main/resources/templates/vehicles/form.html:23-158`
  - Atributos declarativos, classes de grid e carregamento como ES module.
- Modify: `src/main/resources/static/css/app.css` na região dos seletores `.vehicle-form-*`
  - Grades proporcionais, foco/erro do `input-group` e refinamento de espaçamento.
- Modify: `src/main/java/dev/harrison/rendacomcarro/vehicle/web/VehicleForm.java:50-82`
  - Normalização defensiva de espaços também no servidor.
- Create: `src/test/java/dev/harrison/rendacomcarro/vehicle/web/VehicleFormTest.java`
  - Teste unitário da normalização textual e da placa.
- Modify: `src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleWebTest.java:48-123`
  - Contrato HTML e persistência dos valores brasileiros em criação e edição.

---

### Task 1: Pure vehicle input formatters

**Files:**
- Create: `src/main/resources/static/js/vehicle-form-inputs.js`
- Create: `src/test/js/vehicle-form-inputs.test.mjs`

**Interfaces:**
- Produces: `formatMoneyInput(raw, maxDigits) -> string`
- Produces: `formatOdometerInput(raw, options) -> string`
- Produces: `formatVehiclePlate(raw) -> string`
- Produces: `normalizeVehicleText(raw) -> string`
- Consumes: somente strings e números simples; não acessa DOM.

- [ ] **Step 1: Write the failing formatter tests**

Create `src/test/js/vehicle-form-inputs.test.mjs`:

```js
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
```

- [ ] **Step 2: Run the formatter test and confirm the red state**

Run:

```bash
node --test src/test/js/vehicle-form-inputs.test.mjs
```

Expected: FAIL with `ERR_MODULE_NOT_FOUND` for `vehicle-form-inputs.js`.

- [ ] **Step 3: Implement the pure formatter module**

Create `src/main/resources/static/js/vehicle-form-inputs.js`:

```js
const DEFAULT_MONEY_DIGITS = 14;
const DEFAULT_ODOMETER_INTEGER_DIGITS = 11;

const asText = (value) => String(value ?? '');
const digitsOnly = (value) => asText(value).replace(/\D/g, '');

const trimLeadingZeros = (digits) => {
  const normalized = digits.replace(/^0+(?=\d)/, '');
  return normalized || '0';
};

const groupThousands = (digits) => trimLeadingZeros(digits)
  .replace(/\B(?=(\d{3})+(?!\d))/g, '.');

export const formatMoneyInput = (raw, maxDigits = DEFAULT_MONEY_DIGITS) => {
  const digits = digitsOnly(raw).slice(0, maxDigits);
  if (!digits) return '';

  const padded = digits.padStart(3, '0');
  const integerDigits = padded.slice(0, -2);
  const cents = padded.slice(-2);
  return `${groupThousands(integerDigits)},${cents}`;
};

const splitOdometer = (raw) => {
  const source = asText(raw)
    .replace(/\s/g, '')
    .replace(/[^\d,.]/g, '');

  if (!source) {
    return { integerSource: '', decimalSource: '', hasDecimalSeparator: false };
  }

  const commaIndex = source.lastIndexOf(',');
  if (commaIndex >= 0) {
    return {
      integerSource: source.slice(0, commaIndex),
      decimalSource: source.slice(commaIndex + 1),
      hasDecimalSeparator: true
    };
  }

  const dotCount = (source.match(/\./g) || []).length;
  const dotIndex = source.lastIndexOf('.');
  const digitsAfterDot = dotIndex >= 0 ? source.length - dotIndex - 1 : -1;
  const groupedInteger = /^\d{1,3}(\.\d{3})+$/.test(source);
  const dotActsAsDecimal = dotCount === 1
    && dotIndex > 0
    && digitsAfterDot <= 1
    && !groupedInteger;

  if (dotActsAsDecimal) {
    return {
      integerSource: source.slice(0, dotIndex),
      decimalSource: source.slice(dotIndex + 1),
      hasDecimalSeparator: true
    };
  }

  return {
    integerSource: source,
    decimalSource: '',
    hasDecimalSeparator: false
  };
};

export const formatOdometerInput = (
  raw,
  {
    maxIntegerDigits = DEFAULT_ODOMETER_INTEGER_DIGITS,
    trimZeroFraction = false
  } = {}
) => {
  const { integerSource, decimalSource, hasDecimalSeparator } = splitOdometer(raw);
  const integerDigits = digitsOnly(integerSource).slice(0, maxIntegerDigits);

  if (!integerDigits && !hasDecimalSeparator) return '';

  const groupedInteger = groupThousands(integerDigits || '0');
  if (!hasDecimalSeparator) return groupedInteger;

  const decimalDigit = digitsOnly(decimalSource).slice(0, 1);
  if (trimZeroFraction && (!decimalDigit || decimalDigit === '0')) {
    return groupedInteger;
  }

  return `${groupedInteger},${decimalDigit}`;
};

export const formatVehiclePlate = (raw) => {
  const normalized = asText(raw)
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '')
    .slice(0, 7);

  if (/^[A-Z]{3}\d{1,4}$/.test(normalized) && normalized.length > 3) {
    return `${normalized.slice(0, 3)}-${normalized.slice(3)}`;
  }

  return normalized;
};

export const normalizeVehicleText = (raw) => asText(raw)
  .trim()
  .replace(/\s+/g, ' ');
```

- [ ] **Step 4: Run the formatter tests and confirm green**

Run:

```bash
node --test src/test/js/vehicle-form-inputs.test.mjs
```

Expected: 6 tests PASS, 0 failures.

- [ ] **Step 5: Run the complete JavaScript suite**

Run:

```bash
npm run test:js
```

Expected: all existing and new JavaScript tests PASS.

- [ ] **Step 6: Commit the formatter module**

```bash
git add src/main/resources/static/js/vehicle-form-inputs.js \
  src/test/js/vehicle-form-inputs.test.mjs
git commit -m "feat: add vehicle input formatters"
```

---

### Task 2: DOM integration, validation and submit state

**Files:**
- Modify: `src/main/resources/static/js/vehicle-form.js:1-88`
- Create: `src/test/js/vehicle-form.test.mjs`

**Interfaces:**
- Consumes: `formatMoneyInput`, `formatOdometerInput`, `formatVehiclePlate`, `normalizeVehicleText` from Task 1.
- Produces: `setVehicleSubmitState(button, submitting) -> void`
- Produces: `initializeVehicleForm(documentObject, windowObject) -> object | null`
- The initializer reads only elements inside `#vehicleForm` and accepts injected document/window objects for tests.

- [ ] **Step 1: Write the failing DOM-state tests**

Create `src/test/js/vehicle-form.test.mjs`:

```js
import test from 'node:test';
import assert from 'node:assert/strict';

import { initializeVehicleForm } from '../../main/resources/static/js/vehicle-form.js';

const createClassList = (initial = []) => {
  const values = new Set(initial);
  return {
    add: (value) => values.add(value),
    remove: (value) => values.delete(value),
    contains: (value) => values.has(value)
  };
};

const createHarness = ({ valid }) => {
  const formListeners = new Map();
  const windowListeners = new Map();
  const group = { classList: createClassList() };
  const invalidInput = {
    classList: createClassList(valid ? [] : ['is-invalid']),
    focused: false,
    attributes: new Map(),
    checkValidity: () => valid,
    closest: (selector) => selector === '.input-group' ? group : null,
    focus() { this.focused = true; },
    setAttribute(name, value) { this.attributes.set(name, value); },
    removeAttribute(name) { this.attributes.delete(name); }
  };
  const button = {
    disabled: false,
    textContent: 'Cadastrar veículo',
    dataset: {}
  };

  const selectorResults = new Map([
    ['[data-vehicle-plate]', []],
    ['[data-money-input]', []],
    ['[data-odometer-input]', []],
    ['[data-normalize-spaces]', []],
    ['.is-invalid', valid ? [] : [invalidInput]],
    [':invalid, .is-invalid', valid ? [] : [invalidInput]]
  ]);

  const form = {
    checkValidity: () => valid,
    querySelector: (selector) => selector === '[data-vehicle-submit]' ? button : null,
    querySelectorAll: (selector) => selectorResults.get(selector) || [],
    addEventListener: (type, listener) => formListeners.set(type, listener)
  };

  const documentObject = {
    querySelector: (selector) => selector === '#vehicleForm' ? form : null
  };
  const windowObject = {
    addEventListener: (type, listener) => windowListeners.set(type, listener),
    requestAnimationFrame: (callback) => callback()
  };

  return {
    button,
    formListeners,
    invalidInput,
    windowListeners,
    documentObject,
    windowObject
  };
};

test('vehicle form: invalid submit keeps button enabled and focuses first invalid field', () => {
  const harness = createHarness({ valid: false });
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  harness.formListeners.get('submit')({});

  assert.equal(harness.button.disabled, false);
  assert.equal(harness.button.textContent, 'Cadastrar veículo');
  assert.equal(harness.invalidInput.focused, true);
  assert.equal(harness.invalidInput.attributes.get('aria-invalid'), 'true');
});

test('vehicle form: pageshow restores submit button after a valid submission', () => {
  const harness = createHarness({ valid: true });
  initializeVehicleForm(harness.documentObject, harness.windowObject);

  harness.formListeners.get('submit')({});
  assert.equal(harness.button.disabled, true);
  assert.equal(harness.button.textContent, 'Salvando…');

  harness.windowListeners.get('pageshow')({});
  assert.equal(harness.button.disabled, false);
  assert.equal(harness.button.textContent, 'Cadastrar veículo');
});
```

- [ ] **Step 2: Run the DOM-state tests and confirm the red state**

Run:

```bash
node --test src/test/js/vehicle-form.test.mjs
```

Expected: FAIL because `vehicle-form.js` does not export `initializeVehicleForm` and is not yet an ES module.

- [ ] **Step 3: Replace the current IIFE with an injectable ES module**

Replace `src/main/resources/static/js/vehicle-form.js` with:

```js
import {
  formatMoneyInput,
  formatOdometerInput,
  formatVehiclePlate,
  normalizeVehicleText
} from './vehicle-form-inputs.js';

const setCaretToEnd = (input) => {
  if (typeof input.setSelectionRange !== 'function') return;
  const end = input.value.length;
  input.setSelectionRange(end, end);
};

const syncInputValidity = (input) => {
  if (!input) return;

  const invalidByServer = input.classList?.contains('is-invalid') ?? false;
  const invalidByBrowser = typeof input.checkValidity === 'function'
    ? !input.checkValidity()
    : false;
  const invalid = invalidByServer || invalidByBrowser;
  const group = input.closest?.('.input-group');

  if (invalid) {
    input.setAttribute?.('aria-invalid', 'true');
    group?.classList.add('is-invalid-group');
    return;
  }

  input.removeAttribute?.('aria-invalid');
  input.classList?.remove('is-invalid');
  group?.classList.remove('is-invalid-group');
};

const markInvalidFields = (form, windowObject) => {
  const invalidInputs = Array.from(form.querySelectorAll(':invalid, .is-invalid'));
  invalidInputs.forEach(syncInputValidity);

  if (invalidInputs.length > 0) {
    windowObject.requestAnimationFrame(() => invalidInputs[0].focus());
  }

  return invalidInputs;
};

export const setVehicleSubmitState = (button, submitting) => {
  if (!button) return;

  if (submitting) {
    button.dataset.originalText ||= button.textContent;
    button.disabled = true;
    button.textContent = 'Salvando…';
    return;
  }

  button.disabled = false;
  if (button.dataset.originalText) {
    button.textContent = button.dataset.originalText;
  }
};

export const initializeVehicleForm = (
  documentObject = document,
  windowObject = window
) => {
  const form = documentObject.querySelector('#vehicleForm');
  if (!form) return null;

  const submitButton = form.querySelector('[data-vehicle-submit]');
  const plateInputs = Array.from(form.querySelectorAll('[data-vehicle-plate]'));
  const moneyInputs = Array.from(form.querySelectorAll('[data-money-input]'));
  const odometerInputs = Array.from(form.querySelectorAll('[data-odometer-input]'));
  const textInputs = Array.from(form.querySelectorAll('[data-normalize-spaces]'));
  let dirty = false;
  let submitting = false;

  plateInputs.forEach((input) => {
    input.value = formatVehiclePlate(input.value);
    input.addEventListener('input', () => {
      input.value = formatVehiclePlate(input.value);
      setCaretToEnd(input);
    });
  });

  moneyInputs.forEach((input) => {
    input.value = formatMoneyInput(
      input.value,
      Number(input.dataset.maxDigits || '14')
    );
    input.addEventListener('input', () => {
      input.value = formatMoneyInput(
        input.value,
        Number(input.dataset.maxDigits || '14')
      );
      setCaretToEnd(input);
    });
  });

  odometerInputs.forEach((input) => {
    const maxIntegerDigits = Number(input.dataset.maxIntegerDigits || '11');
    input.value = formatOdometerInput(input.value, {
      maxIntegerDigits,
      trimZeroFraction: true
    });
    input.addEventListener('input', () => {
      input.value = formatOdometerInput(input.value, { maxIntegerDigits });
      setCaretToEnd(input);
    });
    input.addEventListener('blur', () => {
      input.value = formatOdometerInput(input.value, {
        maxIntegerDigits,
        trimZeroFraction: true
      });
    });
  });

  textInputs.forEach((input) => {
    input.addEventListener('blur', () => {
      input.value = normalizeVehicleText(input.value);
    });
  });

  Array.from(form.querySelectorAll('.is-invalid')).forEach(syncInputValidity);

  form.addEventListener('invalid', (event) => {
    syncInputValidity(event.target);
  }, true);

  form.addEventListener('input', (event) => {
    dirty = true;
    syncInputValidity(event.target);
  });

  form.addEventListener('submit', () => {
    textInputs.forEach((input) => {
      input.value = normalizeVehicleText(input.value);
    });
    odometerInputs.forEach((input) => {
      input.value = formatOdometerInput(input.value, {
        maxIntegerDigits: Number(input.dataset.maxIntegerDigits || '11'),
        trimZeroFraction: true
      });
    });

    if (!form.checkValidity()) {
      submitting = false;
      setVehicleSubmitState(submitButton, false);
      markInvalidFields(form, windowObject);
      return;
    }

    submitting = true;
    dirty = false;
    setVehicleSubmitState(submitButton, true);
  });

  windowObject.addEventListener('pageshow', () => {
    submitting = false;
    setVehicleSubmitState(submitButton, false);
  });

  windowObject.addEventListener('beforeunload', (event) => {
    if (!dirty || submitting) return;
    event.preventDefault();
    event.returnValue = '';
  });

  return { form, submitButton };
};

if (typeof document !== 'undefined') {
  initializeVehicleForm(document, window);
}
```

- [ ] **Step 4: Run the DOM-state tests and confirm green**

Run:

```bash
node --test src/test/js/vehicle-form.test.mjs
```

Expected: 2 tests PASS, 0 failures.

- [ ] **Step 5: Run the complete JavaScript suite**

Run:

```bash
npm run test:js
```

Expected: all JavaScript tests PASS.

- [ ] **Step 6: Commit the DOM integration**

```bash
git add src/main/resources/static/js/vehicle-form.js \
  src/test/js/vehicle-form.test.mjs
git commit -m "feat: wire smart vehicle form inputs"
```

---

### Task 3: Declarative form markup and proportional CSS grids

**Files:**
- Modify: `src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleWebTest.java:48-61`
- Modify: `src/main/resources/templates/vehicles/form.html:23-158`
- Modify: `src/main/resources/static/css/app.css` na região `.vehicle-form-*`

**Interfaces:**
- Consumes: data attributes recognized by `initializeVehicleForm` from Task 2.
- Produces: `data-money-input`, `data-odometer-input`, `data-normalize-spaces`, `data-max-digits`, `data-max-integer-digits` and module script contract.
- Produces: CSS classes `vehicle-identification-grid`, `vehicle-operation-grid`, `vehicle-acquisition-grid` and existing `vehicle-year-plate-grid`.

- [ ] **Step 1: Extend the rendered-form contract test**

In `newVehicleFormUsesGroupedResponsiveLayout()` add these expectations before the final semicolon:

```java
            .andExpect(content().string(containsString("vehicle-identification-grid")))
            .andExpect(content().string(containsString("vehicle-operation-grid")))
            .andExpect(content().string(containsString("vehicle-acquisition-grid")))
            .andExpect(content().string(containsString("data-normalize-spaces")))
            .andExpect(content().string(containsString("data-odometer-input")))
            .andExpect(content().string(containsString("data-money-input")))
            .andExpect(content().string(containsString("data-max-digits=\"14\"")))
            .andExpect(content().string(containsString("data-max-integer-digits=\"11\"")))
            .andExpect(content().string(containsString("type=\"module\"")))
```

Keep the existing assertion that `>Salvar veículo<` is absent.

- [ ] **Step 2: Run the contract test and confirm the red state**

Run:

```bash
./mvnw -Dtest=VehicleWebTest#newVehicleFormUsesGroupedResponsiveLayout test
```

Expected: FAIL because the new data attributes, classes and `type="module"` are not present.

- [ ] **Step 3: Add declarative attributes to text and numeric fields**

Update the inputs in `src/main/resources/templates/vehicles/form.html` so their opening tags contain these final attributes:

```html
<input class="form-control" id="name" th:field="*{name}" maxlength="120"
       placeholder="Ex.: Sandero principal" data-normalize-spaces
       th:classappend="${#fields.hasErrors('name')} ? ' is-invalid'"
       aria-describedby="nameHelp nameError" autocomplete="off">

<input class="form-control" id="make" th:field="*{make}" maxlength="80" required
       placeholder="Ex.: Renault" data-normalize-spaces
       th:classappend="${#fields.hasErrors('make')} ? ' is-invalid'"
       aria-describedby="makeError" autocomplete="organization">

<input class="form-control" id="model" th:field="*{model}" maxlength="80" required
       placeholder="Ex.: Sandero" data-normalize-spaces
       th:classappend="${#fields.hasErrors('model')} ? ' is-invalid'"
       aria-describedby="modelError" autocomplete="off">

<input class="form-control" id="initialOdometer" th:field="*{initialOdometer}"
       type="text" inputmode="decimal" required placeholder="0"
       data-odometer-input data-max-integer-digits="11"
       th:classappend="${#fields.hasErrors('initialOdometer')} ? ' is-invalid'"
       aria-describedby="odometerHelp odometerError">

<input class="form-control" id="purchasePrice" th:field="*{purchasePrice}"
       type="text" inputmode="numeric" placeholder="0,00"
       data-money-input data-max-digits="14"
       th:classappend="${#fields.hasErrors('purchasePrice')} ? ' is-invalid'"
       aria-describedby="purchasePriceHelp purchasePriceError">
```

Remove `data-localized-number` and `data-decimal-scale` from the odometer and price inputs. Use `inputmode="numeric"` for the card-terminal price because only digits are meaningful during typing.

- [ ] **Step 4: Replace Bootstrap half-width wrappers with semantic proportional grids**

Keep the nickname and year/plate blocks as direct children of the existing identification `.row`, but replace the separate make/model columns with this complete block:

```html
<div class="col-12">
  <div class="vehicle-identification-grid">
    <div>
      <label class="form-label" for="make">Marca <span class="required-mark" aria-hidden="true">*</span></label>
      <input class="form-control" id="make" th:field="*{make}" maxlength="80" required
             placeholder="Ex.: Renault" data-normalize-spaces
             th:classappend="${#fields.hasErrors('make')} ? ' is-invalid'"
             aria-describedby="makeError" autocomplete="organization">
      <div class="invalid-feedback" id="makeError" th:errors="*{make}"></div>
    </div>

    <div>
      <label class="form-label" for="model">Modelo <span class="required-mark" aria-hidden="true">*</span></label>
      <input class="form-control" id="model" th:field="*{model}" maxlength="80" required
             placeholder="Ex.: Sandero" data-normalize-spaces
             th:classappend="${#fields.hasErrors('model')} ? ' is-invalid'"
             aria-describedby="modelError" autocomplete="off">
      <div class="invalid-feedback" id="modelError" th:errors="*{model}"></div>
    </div>
  </div>
</div>
```

Replace the operation `.row g-3` wrapper with:

```html
<div class="vehicle-operation-grid">
  <div>
    <label class="form-label" for="fuelType">Combustível <span class="required-mark" aria-hidden="true">*</span></label>
    <select class="form-select" id="fuelType" th:field="*{fuelType}" required
            th:classappend="${#fields.hasErrors('fuelType')} ? ' is-invalid'"
            aria-describedby="fuelTypeError">
      <option value="">Selecione o combustível</option>
      <option th:each="type : ${fuelTypes}" th:value="${type}" th:text="${type.label}"></option>
    </select>
    <div class="invalid-feedback" id="fuelTypeError" th:errors="*{fuelType}"></div>
  </div>

  <div>
    <label class="form-label" for="initialOdometer">
      <span th:text="${editing ? 'Odômetro atual' : 'Odômetro inicial'}"></span>
      <span class="required-mark" aria-hidden="true">*</span>
    </label>
    <div class="input-group input-unit">
      <input class="form-control" id="initialOdometer" th:field="*{initialOdometer}"
             type="text" inputmode="decimal" required placeholder="0"
             data-odometer-input data-max-integer-digits="11"
             th:classappend="${#fields.hasErrors('initialOdometer')} ? ' is-invalid'"
             aria-describedby="odometerHelp odometerError">
      <span class="input-group-text">km</span>
      <div class="invalid-feedback" id="odometerError" th:errors="*{initialOdometer}"></div>
    </div>
    <div class="form-text" id="odometerHelp">Usado para calcular a quilometragem percorrida e os custos do veículo.</div>
  </div>
</div>
```

Replace the acquisition `.row g-3` wrapper with:

```html
<div class="vehicle-acquisition-grid">
  <div>
    <label class="form-label" for="purchasePrice">Preço de compra <span class="text-secondary fw-normal">(opcional)</span></label>
    <div class="input-group input-unit">
      <span class="input-group-text">R$</span>
      <input class="form-control" id="purchasePrice" th:field="*{purchasePrice}"
             type="text" inputmode="numeric" placeholder="0,00"
             data-money-input data-max-digits="14"
             th:classappend="${#fields.hasErrors('purchasePrice')} ? ' is-invalid'"
             aria-describedby="purchasePriceHelp purchasePriceError">
      <div class="invalid-feedback" id="purchasePriceError" th:errors="*{purchasePrice}"></div>
    </div>
    <div class="form-text" id="purchasePriceHelp">Pode ser preenchido depois, quando a informação estiver disponível.</div>
  </div>
</div>
```

- [ ] **Step 5: Load the form script as an ES module**

Replace the final script tag with:

```html
<script type="module" th:src="@{/js/vehicle-form.js}"></script>
```

- [ ] **Step 6: Replace the vehicle form CSS block with proportional grids and grouped focus states**

In `src/main/resources/static/css/app.css`, replace the selectors from `.vehicle-form-page` through the old `@media (min-width: 390px)` block with:

```css
.vehicle-form-page { max-width: 57.5rem; }
.vehicle-form-back { color: var(--bs-secondary-color); font-weight: 600; }
.vehicle-form-back:hover { color: var(--bs-primary); }
.vehicle-form-back > span:first-child { font-size: 1.5rem; line-height: 1; }
.vehicle-form { overflow: visible; }
.vehicle-form-section { padding: 1.25rem 1.5rem; }
.vehicle-form-section__header { margin-bottom: 1rem; }
.vehicle-identification-grid,
.vehicle-year-plate-grid,
.vehicle-operation-grid,
.vehicle-acquisition-grid {
  display: grid;
  gap: 1rem;
  align-items: start;
}
.vehicle-identification-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.vehicle-year-plate-grid {
  grid-template-columns: minmax(7ch, .42fr) minmax(12ch, 1.58fr);
}
.vehicle-operation-grid {
  grid-template-columns: minmax(0, .85fr) minmax(0, 1.15fr);
}
.vehicle-acquisition-grid {
  grid-template-columns: minmax(0, 3fr) minmax(0, 2fr);
}
.vehicle-acquisition-grid > * { grid-column: 1; }
.vehicle-form .form-label { font-weight: 600; }
.vehicle-form .form-control,
.vehicle-form .form-select,
.vehicle-form .input-group-text { min-height: 44px; }
.vehicle-form .form-text { max-width: 42rem; }
.vehicle-form .input-group:focus-within {
  border-radius: var(--bs-border-radius);
  outline: 3px solid rgba(13, 110, 253, .25);
  outline-offset: 1px;
}
.vehicle-form .input-group .form-control:focus {
  box-shadow: none;
  outline: 0;
}
.vehicle-form .input-group.is-invalid-group {
  border-radius: var(--bs-border-radius);
  outline: 2px solid rgba(var(--bs-danger-rgb), .35);
  outline-offset: 1px;
}
.vehicle-form-actions { background: var(--bs-body-bg); }

@media (max-width: 47.99rem) {
  .vehicle-identification-grid,
  .vehicle-operation-grid,
  .vehicle-acquisition-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .vehicle-acquisition-grid > * { grid-column: auto; }
}

@media (max-width: 22rem) {
  .vehicle-year-plate-grid { grid-template-columns: minmax(0, 1fr); }
}
```

Keep the existing `@media (max-width: 767.98px)` mobile action-footer rules. Inside that block, keep `.vehicle-form-section { padding: 1.1rem 1rem; }` so mobile spacing remains compact.

- [ ] **Step 7: Run the rendered-form contract test**

Run:

```bash
./mvnw -Dtest=VehicleWebTest#newVehicleFormUsesGroupedResponsiveLayout test
```

Expected: PASS.

- [ ] **Step 8: Run JavaScript tests after the module/template change**

Run:

```bash
npm run test:js
```

Expected: all JavaScript tests PASS.

- [ ] **Step 9: Commit markup and styling**

```bash
git add src/main/resources/templates/vehicles/form.html \
  src/main/resources/static/css/app.css \
  src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleWebTest.java
git commit -m "style: refine vehicle form proportions"
```

---

### Task 4: Server-side text normalization and localized persistence coverage

**Files:**
- Create: `src/test/java/dev/harrison/rendacomcarro/vehicle/web/VehicleFormTest.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/vehicle/web/VehicleForm.java:50-82`
- Modify: `src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleWebTest.java`

**Interfaces:**
- Consumes: localized values already supported by `BrazilianBigDecimalEditor`.
- Produces: `VehicleForm` setters that collapse repeated whitespace without changing letter case.
- Preserves: blank optional name becomes `null`, blank price remains `null`, and plate normalization remains uppercase without spaces/hyphens.

- [ ] **Step 1: Write the failing VehicleForm normalization test**

Create `src/test/java/dev/harrison/rendacomcarro/vehicle/web/VehicleFormTest.java`:

```java
package dev.harrison.rendacomcarro.vehicle.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleFormTest {
    @Test
    void normalizesWhitespaceWithoutChangingCapitalization() {
        VehicleForm form = new VehicleForm();

        form.setName("  Sandero   principal  ");
        form.setMake("  Land\u00A0  Rover  ");
        form.setModel("  CR-V  ");

        assertThat(form.getName()).isEqualTo("Sandero principal");
        assertThat(form.getMake()).isEqualTo("Land Rover");
        assertThat(form.getModel()).isEqualTo("CR-V");
    }

    @Test
    void keepsBlankNicknameNullAndNormalizesBothPlatePresentations() {
        VehicleForm form = new VehicleForm();

        form.setName("   ");
        form.setPlate(" abc-1234 ");

        assertThat(form.getName()).isNull();
        assertThat(form.getPlate()).isEqualTo("ABC1234");

        form.setPlate("abc1d23");
        assertThat(form.getPlate()).isEqualTo("ABC1D23");
    }
}
```

- [ ] **Step 2: Run the unit test and confirm the red state**

Run:

```bash
./mvnw -Dtest=dev.harrison.rendacomcarro.vehicle.web.VehicleFormTest test
```

Expected: FAIL because repeated internal spaces are not collapsed by the current `trimToNull` implementation.

- [ ] **Step 3: Normalize text defensively in VehicleForm setters**

In `VehicleForm.java`, change the three text setters to:

```java
public void setName(String name) { this.name = normalizeText(name); }
public void setMake(String make) { this.make = normalizeText(make); }
public void setModel(String model) { this.model = normalizeText(model); }
```

Replace `trimToNull` with:

```java
private static String normalizeText(String value) {
    if (value == null) {
        return null;
    }
    String normalized = value
        .replace('\u00A0', ' ')
        .trim()
        .replaceAll("\\s+", " ");
    return normalized.isEmpty() ? null : normalized;
}
```

Do not change `normalizePlate`.

- [ ] **Step 4: Run the VehicleForm unit test and confirm green**

Run:

```bash
./mvnw -Dtest=dev.harrison.rendacomcarro.vehicle.web.VehicleFormTest test
```

Expected: 2 tests PASS.

- [ ] **Step 5: Add creation and editing persistence coverage**

Add this test to `VehicleWebTest.java`:

```java
@Test
@WithMockUser(username = "harrison", roles = "OWNER")
void maskedBrazilianValuesPersistOnCreateAndEdit() throws Exception {
    mvc.perform(post("/vehicles")
            .with(csrf())
            .param("name", "  Sandero   principal  ")
            .param("make", "Renault")
            .param("model", "Sandero")
            .param("year", "2013")
            .param("plate", "mno-4321")
            .param("fuelType", "FLEX")
            .param("initialOdometer", "248.351,5")
            .param("purchasePrice", "23.990,00"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/vehicles"));

    var created = vehicleRepository.findAll().stream()
        .filter(candidate -> candidate.getPlate().equals("MNO4321"))
        .findFirst()
        .orElseThrow();

    assertThat(created.getName()).isEqualTo("Sandero principal");
    assertThat(created.getCurrentOdometer()).isEqualByComparingTo("248351.5");
    assertThat(created.getPurchasePrice()).isEqualByComparingTo("23990.00");

    mvc.perform(post("/vehicles/{id}", created.getId())
            .with(csrf())
            .param("name", "Sandero principal")
            .param("make", "Renault")
            .param("model", "Sandero")
            .param("year", "2013")
            .param("plate", "mno4p21")
            .param("fuelType", "FLEX")
            .param("initialOdometer", "248.352")
            .param("purchasePrice", "24.500,50"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/vehicles/" + created.getId()));

    var updated = vehicleRepository.findById(created.getId()).orElseThrow();
    assertThat(updated.getPlate()).isEqualTo("MNO4P21");
    assertThat(updated.getCurrentOdometer()).isEqualByComparingTo("248352.0");
    assertThat(updated.getPurchasePrice()).isEqualByComparingTo("24500.50");
}
```

- [ ] **Step 6: Run the focused web tests**

Run:

```bash
./mvnw -Dtest=VehicleWebTest test
```

Expected: all `VehicleWebTest` cases PASS, including blank price as `NULL`, old plate creation and Mercosul plate editing.

- [ ] **Step 7: Commit server normalization and integration tests**

```bash
git add src/main/java/dev/harrison/rendacomcarro/vehicle/web/VehicleForm.java \
  src/test/java/dev/harrison/rendacomcarro/vehicle/web/VehicleFormTest.java \
  src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleWebTest.java
git commit -m "test: cover localized vehicle form persistence"
```

---

### Task 5: Full verification and responsive acceptance check

**Files:**
- Verify only; no new production file.

**Interfaces:**
- Consumes: all outputs from Tasks 1-4.
- Produces: evidence that JavaScript, Java, packaging and responsive behavior remain valid.

- [ ] **Step 1: Check the diff for whitespace and accidental generated files**

Run:

```bash
git diff --check main...HEAD
git status --short
```

Expected: `git diff --check` exits 0; status contains only intentional source, test and documentation files.

- [ ] **Step 2: Run all JavaScript tests**

Run:

```bash
npm run test:js
```

Expected: all tests PASS.

- [ ] **Step 3: Run focused vehicle tests**

Run:

```bash
./mvnw -Dtest=VehicleFormTest,VehicleWebTest test
```

Expected: all focused tests PASS.

- [ ] **Step 4: Run the complete Java test suite**

Run:

```bash
./mvnw clean test
```

Expected: BUILD SUCCESS with 0 test failures and 0 errors.

- [ ] **Step 5: Package the application**

Run:

```bash
./mvnw -DskipTests package
```

Expected: BUILD SUCCESS and an executable JAR under `target/`.

- [ ] **Step 6: Start the application through the existing Compose stack**

Run:

```bash
docker compose up -d --build postgres app
curl --fail --silent http://127.0.0.1:8080/actuator/health
```

Expected: health response contains `"status":"UP"`.

- [ ] **Step 7: Verify the form manually at representative widths**

Open `/vehicles/new` with the configured owner credentials and verify in browser DevTools at widths 320, 390, 768 and 1440:

```text
320: ano/placa may stack only when the usable content area cannot hold both fields.
390: ano and placa remain side by side; placa receives the larger fraction.
768: marca/modelo and combustível/odômetro use balanced proportional columns.
1440: preço occupies only the first 3/5 of the acquisition grid instead of half/full width.
```

Exercise these inputs:

```text
Preço: 1 -> 0,01; 123 -> 1,23; 2399000 -> 23.990,00; delete all -> blank.
Odômetro: 248351 -> 248.351; 248351,5 -> 248.351,5; blur 248351,0 -> 248.351.
Placa: abc1234 -> ABC-1234; abc1d23 -> ABC1D23.
Texto: "  Land   Rover  " -> "Land Rover" without changing case.
Invalid submit: button remains enabled and first invalid field receives focus.
Browser back after submit: button returns from "Salvando…" to its original label.
```

- [ ] **Step 8: Stop the local stack**

Run:

```bash
docker compose down --remove-orphans
```

Expected: containers stop cleanly.

- [ ] **Step 9: Review commit history before opening the PR**

Run:

```bash
git log --oneline main..HEAD
git status --short
```

Expected: the plan/spec commits plus four focused implementation commits are present; working tree is clean.

---

## Self-Review Results

- **Spec coverage:** dinheiro, odômetro, placas, limpeza de texto, proporções, estados de erro, envio inválido, `pageshow`, alterações não salvas, testes JS, testes web e CI estão mapeados para tarefas explícitas.
- **Placeholder scan:** o plano não contém `TBD`, `TODO`, “implementar depois” ou etapas sem comando/código.
- **Type consistency:** os exports de `vehicle-form-inputs.js` usados por `vehicle-form.js` e pelos testes possuem os mesmos nomes e assinaturas.
- **Precision consistency:** preço usa 14 dígitos totais para `precision=14, scale=2`; odômetro usa 11 dígitos inteiros e uma casa para `precision=12, scale=1`.
- **Dependency consistency:** os testes usam apenas `node:test`, já suportado pelo script `npm run test:js`; nenhuma dependência npm é adicionada.
