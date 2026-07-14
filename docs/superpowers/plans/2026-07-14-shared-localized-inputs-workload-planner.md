# Shared Localized Inputs and Workload Planner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Padronizar máscaras brasileiras nos formulários guiados e substituir o campo mensal bruto de horas por uma jornada diária, semanal ou mensal calculada a partir dos dias planejados.

**Architecture:** Extrair as transformações numéricas e a integração com o DOM para módulos ES compartilhados e declarativos. Implementar a jornada em minutos inteiros, com cálculo puro equivalente no JavaScript para prévia e um serviço Java autoritativo para persistência, mantendo `planned_hours` apenas como representação derivada de compatibilidade.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Data JPA, Bean Validation, Thymeleaf, Bootstrap 5.3.7, PostgreSQL 17, Flyway, JavaScript ES modules, Node.js 22 com `node:test`, JUnit 5, MockMvc e Testcontainers.

## Global Constraints

- Trabalhar exclusivamente na branch existente `fix/guided-draft-save-race`, vinculada ao PR #9.
- Não adicionar biblioteca externa de máscaras, datas ou componentes de horário.
- Manter placa e regras exclusivas de veículo no módulo de veículo.
- Dinheiro usa semântica de maquininha com duas casas implícitas: `1` → `0,01`.
- Percentual usa duas casas implícitas, símbolo externo e intervalo validado de `0,00` a `100,00`.
- Odômetro usa agrupamento brasileiro e no máximo uma casa decimal opcional.
- Contagens permanecem inteiras.
- Jornada aceita `DAILY`, `WEEKLY` e `MONTHLY`, sempre com horas e minutos.
- Cálculos de jornada usam minutos inteiros; não usar `double` ou horas decimais como fonte de verdade.
- A semana vai de segunda a domingo e o mês selecionado define o período autoritativo.
- Sem evidência de semana interna, uma semana parcial usa referência de cinco dias.
- O backend recalcula a jornada e ignora qualquer total derivado enviado pelo navegador.
- Rascunhos preservam modo e duração originais e continuam usando a fila serializada do PR #9.
- O PR não deve ser mesclado automaticamente.
- Implementação em TDD, com teste vermelho confirmado antes do código de produção e commits pequenos por tarefa.

---

## File Map

### Shared localized inputs

- Create: `src/main/resources/static/js/localized-input-formatters.js`
  - Funções puras para dinheiro, percentual, odômetro, parsing pt-BR e normalização de espaços.
- Create: `src/main/resources/static/js/localized-inputs.js`
  - Inicialização DOM idempotente por atributos `data-*` e integração com restauração de rascunho.
- Create: `src/test/js/localized-input-formatters.test.mjs`
- Create: `src/test/js/localized-inputs.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form-inputs.js`
  - Manter apenas `formatVehiclePlate`.
- Modify: `src/main/resources/static/js/vehicle-form.js`
  - Delegar entradas genéricas ao inicializador compartilhado.
- Modify: `src/test/js/vehicle-form-inputs.test.mjs`
- Modify: `src/test/js/vehicle-form.test.mjs`
- Modify templates:
  - `src/main/resources/templates/vehicles/form.html`
  - `src/main/resources/templates/expenses/form.html`
  - `src/main/resources/templates/goals/form.html`
  - `src/main/resources/templates/obligations/form.html`
  - `src/main/resources/templates/mileage-closings/form.html`
- Modify scripts:
  - `src/main/resources/static/js/expense-form.js`
  - `src/main/resources/static/js/goal-form.js`
  - `src/main/resources/static/js/obligation-form.js`
  - `src/main/resources/static/js/mileage-closing-form.js`
- Modify web tests:
  - `src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleWebTest.java`
  - `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseWebTest.java`
  - `src/test/java/dev/harrison/rendacomcarro/finance/ObligationWebTest.java`
  - `src/test/java/dev/harrison/rendacomcarro/expense/MonthlyMileageClosingWebTest.java`
  - `src/test/java/dev/harrison/rendacomcarro/web/GuidedFormsWebContractTest.java`

### Workload planner

- Create: `src/main/java/dev/harrison/rendacomcarro/goal/domain/WorkloadPeriodicity.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/goal/application/WorkloadCalculation.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/goal/application/WorkloadPlannerService.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/goal/WorkloadPlannerServiceTest.java`
- Create: `src/main/resources/db/migration/V9__add_goal_workload_planner.sql`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/domain/MonthlyGoal.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/domain/PlannedWorkDay.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/application/GoalService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/application/GoalFormSubmissionService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/web/GoalForm.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/web/GoalController.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/draft/application/definition/MonthlyGoalDraftDefinition.java`
- Create: `src/main/resources/static/js/goal-workload-calculator.js`
- Create: `src/main/resources/static/js/goal-workload-planner.js`
- Create: `src/test/js/goal-workload-calculator.test.mjs`
- Create: `src/test/js/goal-workload-planner.test.mjs`
- Modify: `src/test/java/dev/harrison/rendacomcarro/goal/GoalServiceTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/goal/GoalFormSubmissionServiceTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/goal/GoalWebTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/draft/FormDraftDefinitionTest.java`
- Modify: `src/main/resources/templates/goals/detail.html`

---

### Task 1: Pure localized input formatters

**Files:**
- Create: `src/main/resources/static/js/localized-input-formatters.js`
- Create: `src/test/js/localized-input-formatters.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form-inputs.js`
- Modify: `src/test/js/vehicle-form-inputs.test.mjs`

**Interfaces:**
- Produces: `applyFixedScaleEdit(currentDigits, edit) -> string`
- Produces: `formatFixedScaleInput(raw, options) -> string`
- Produces: `formatMoneyInput(raw, maxDigits = 14) -> string`
- Produces: `formatPercentageInput(raw, maxDigits = 5) -> string`
- Produces: `formatOdometerInput(raw, options) -> string`
- Produces: `parseLocalizedDecimal(raw) -> number | null`
- Produces: `normalizeSpaces(raw) -> string`
- Keeps: `formatVehiclePlate(raw) -> string` in `vehicle-form-inputs.js`

- [ ] **Step 1: Write failing pure formatter tests**

Create `src/test/js/localized-input-formatters.test.mjs` with explicit cases:

```js
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

test('money uses two implicit decimals and remains blank when empty', () => {
  assert.equal(formatMoneyInput(''), '');
  assert.equal(formatMoneyInput('1'), '0,01');
  assert.equal(formatMoneyInput('125'), '1,25');
  assert.equal(formatMoneyInput('2399000'), '23.990,00');
});

test('percentage uses two implicit decimals without silently clamping', () => {
  assert.equal(formatPercentageInput('1'), '0,01');
  assert.equal(formatPercentageInput('125'), '1,25');
  assert.equal(formatPercentageInput('10000'), '100,00');
  assert.equal(formatPercentageInput('10001'), '100,01');
});

test('fixed-scale edits support typing, replacement and deletion to blank', () => {
  assert.equal(applyFixedScaleEdit('', { inputType: 'insertText', data: '1' }), '1');
  assert.equal(applyFixedScaleEdit('123', { inputType: 'deleteContentBackward' }), '12');
  assert.equal(applyFixedScaleEdit('123', { inputType: 'deleteContentBackward', replaceAll: true }), '');
});

test('odometer groups thousands and keeps one optional decimal', () => {
  assert.equal(formatOdometerInput('248351'), '248.351');
  assert.equal(formatOdometerInput('248351,5'), '248.351,5');
  assert.equal(formatOdometerInput('248351,'), '248.351,');
  assert.equal(formatOdometerInput('248351,0', { trimZeroFraction: true }), '248.351');
});

test('localized parser accepts pt-BR display values', () => {
  assert.equal(parseLocalizedDecimal('23.990,00'), 23990);
  assert.equal(parseLocalizedDecimal('1,25'), 1.25);
  assert.equal(parseLocalizedDecimal(''), null);
});

test('space normalization preserves capitalization', () => {
  assert.equal(normalizeSpaces('  Banco   Familiar  '), 'Banco Familiar');
});
```

Update `src/test/js/vehicle-form-inputs.test.mjs` so it imports only `formatVehiclePlate` from the vehicle module and imports generic formatter assertions from the new module.

- [ ] **Step 2: Run the formatter tests and confirm the red state**

Run:

```bash
node --test src/test/js/localized-input-formatters.test.mjs src/test/js/vehicle-form-inputs.test.mjs
```

Expected: FAIL with `ERR_MODULE_NOT_FOUND` for `localized-input-formatters.js`.

- [ ] **Step 3: Implement the shared pure module**

Implement `localized-input-formatters.js` using digit-only internal state, `Intl.NumberFormat`-free deterministic grouping, two implicit decimal places for money/percentage, the existing odometer parser, a single pt-BR parser, and generic whitespace normalization. Do not add DOM access.

The implementation must export the exact interfaces above and preserve the existing money and odometer behavior already covered by PR #7.

Replace `vehicle-form-inputs.js` with only the plate formatter:

```js
const asText = (value) => String(value ?? '');

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
```

- [ ] **Step 4: Run pure formatter tests**

Run:

```bash
node --test src/test/js/localized-input-formatters.test.mjs src/test/js/vehicle-form-inputs.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/js/localized-input-formatters.js \
        src/main/resources/static/js/vehicle-form-inputs.js \
        src/test/js/localized-input-formatters.test.mjs \
        src/test/js/vehicle-form-inputs.test.mjs
git commit -m "refactor: extract localized input formatters"
```

---

### Task 2: Declarative DOM initializer and vehicle migration

**Files:**
- Create: `src/main/resources/static/js/localized-inputs.js`
- Create: `src/test/js/localized-inputs.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form.js`
- Modify: `src/test/js/vehicle-form.test.mjs`
- Modify: `src/main/resources/templates/vehicles/form.html`

**Interfaces:**
- Consumes formatter functions from Task 1.
- Produces: `initializeLocalizedInputs(root = document) -> { initialized: number }`
- Produces idempotent handlers for `data-money-input`, `data-percentage-input`, `data-odometer-input`, and `data-normalize-spaces`.

- [ ] **Step 1: Write failing DOM initializer tests**

Create a small fake-input harness in `src/test/js/localized-inputs.test.mjs` and verify:

```js
test('initializer wires a money input only once', () => {
  const { root, input, listenerCount } = createHarness('data-money-input');
  initializeLocalizedInputs(root);
  initializeLocalizedInputs(root);
  assert.equal(listenerCount(input, 'beforeinput'), 1);
});

test('money beforeinput emits one normal bubbling input event after rendering', () => {
  const { root, input, dispatches } = createHarness('data-money-input');
  initializeLocalizedInputs(root);
  fireBeforeInput(input, 'insertText', '1');
  assert.equal(input.value, '0,01');
  assert.equal(dispatches.filter((event) => event.type === 'input').length, 1);
});

test('restoration formats values without creating autosave events', () => {
  const { root, input, dispatches } = createHarness('data-money-input', '123');
  initializeLocalizedInputs(root);
  root.dispatchEvent({ type: 'guided-form:restored', detail: { form: root } });
  assert.equal(input.value, '1,23');
  assert.equal(dispatches.filter((event) => event.type === 'input').length, 0);
});
```

- [ ] **Step 2: Run the DOM tests and confirm the red state**

```bash
node --test src/test/js/localized-inputs.test.mjs
```

Expected: FAIL with `ERR_MODULE_NOT_FOUND`.

- [ ] **Step 3: Implement `initializeLocalizedInputs`**

Requirements:

- scan the root and descendant fields for supported attributes;
- mark each initialized input with `data-localized-input-initialized="true"`;
- preserve selection replacement and deletion-to-blank for fixed-scale inputs;
- dispatch exactly one bubbling native `input` event for real edits handled through `beforeinput` or `paste`;
- never dispatch synthetic edits while formatting initial or restored values;
- use `data-max-digits` and `data-max-integer-digits` where present;
- normalize short text on blur;
- trim multiline free-form fields only when explicitly marked for outer trim;
- listen for `guided-form:restored` on `document` and format only the restored form subtree.

- [ ] **Step 4: Refactor the vehicle module**

`vehicle-form.js` must import:

```js
import { initializeLocalizedInputs } from './localized-inputs.js';
import { formatVehiclePlate } from './vehicle-form-inputs.js';
```

Remove the money, odometer and text event wiring from `vehicle-form.js`; call `initializeLocalizedInputs(form)` once. Keep plate, validity-group state, dirty state, submit button, pageshow and beforeunload behavior.

Update `vehicle-form.test.mjs` so the dedicated localized-input tests own the terminal typing assertions and the vehicle test continues to cover invalid submit, corrected errors, pageshow, plate and dirty state.

- [ ] **Step 5: Run JavaScript regression tests**

```bash
npm run test:js
```

Expected: all JavaScript tests PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/js/localized-inputs.js \
        src/main/resources/static/js/vehicle-form.js \
        src/main/resources/templates/vehicles/form.html \
        src/test/js/localized-inputs.test.mjs \
        src/test/js/vehicle-form.test.mjs
git commit -m "refactor: share localized input DOM behavior"
```

---

### Task 3: Apply shared masks to guided forms

**Files:**
- Modify templates for expenses, goals, obligations and mileage closings.
- Modify their page scripts.
- Modify guided-form HTML contract and feature web tests.

**Interfaces:**
- Consumes: `initializeLocalizedInputs` and `parseLocalizedDecimal`.
- Produces: consistent `data-*` contracts and summaries based on the shared parser.

- [ ] **Step 1: Add failing rendered-HTML assertions**

Add assertions that rendered forms contain:

```html
<input name="amount" data-money-input>
<input name="professionalFixedAmount" data-money-input>
<input name="professionalPercentagePercent" data-percentage-input>
<input name="personalNetGoal" data-money-input>
<input name="operationalGoal" data-money-input>
<input name="principal" data-money-input>
<input name="annualRatePercent" data-percentage-input>
<input name="plannedInstallment" data-money-input>
<input name="monthlyTarget" data-money-input>
<input name="initialOdometer" data-odometer-input>
<input name="finalOdometer" data-odometer-input>
<input name="professionalKilometers" data-odometer-input>
```

Also assert that `termMonths` remains an integer input without money attributes and short fields such as creditor/reasons receive `data-normalize-spaces`.

- [ ] **Step 2: Run targeted web tests and confirm failure**

```bash
./mvnw -q -Dtest=GuidedFormsWebContractTest,ExpenseWebTest,ObligationWebTest,MonthlyMileageClosingWebTest,GoalWebTest test
```

Expected: FAIL because the new attributes are absent.

- [ ] **Step 3: Add declarative attributes and shared module imports**

Each page module must call `initializeLocalizedInputs(form)` and import `parseLocalizedDecimal` for summaries. Remove local duplicate `parseDecimal` implementations from `expense-form.js`, `goal-form.js`, and `obligation-form.js`.

Do not change backend field names in this task.

- [ ] **Step 4: Add percentage range validation**

Keep `min="0"`, `max="100"` and server-side validation. The formatter must display `100,01` if typed; HTML/backend validation must reject it instead of silently turning it into `100,00`.

- [ ] **Step 5: Run JavaScript and targeted web tests**

```bash
npm run test:js
./mvnw -q -Dtest=GuidedFormsWebContractTest,ExpenseWebTest,ObligationWebTest,MonthlyMileageClosingWebTest,GoalWebTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/templates/expenses/form.html \
        src/main/resources/templates/goals/form.html \
        src/main/resources/templates/obligations/form.html \
        src/main/resources/templates/mileage-closings/form.html \
        src/main/resources/static/js/expense-form.js \
        src/main/resources/static/js/goal-form.js \
        src/main/resources/static/js/obligation-form.js \
        src/main/resources/static/js/mileage-closing-form.js \
        src/test/java/dev/harrison/rendacomcarro/web/GuidedFormsWebContractTest.java \
        src/test/java/dev/harrison/rendacomcarro/expense/ExpenseWebTest.java \
        src/test/java/dev/harrison/rendacomcarro/finance/ObligationWebTest.java \
        src/test/java/dev/harrison/rendacomcarro/expense/MonthlyMileageClosingWebTest.java \
        src/test/java/dev/harrison/rendacomcarro/goal/GoalWebTest.java
git commit -m "feat: standardize guided form input masks"
```

---

### Task 4: Authoritative Java workload calculation

**Files:**
- Create: `WorkloadPeriodicity.java`
- Create: `WorkloadCalculation.java`
- Create: `WorkloadPlannerService.java`
- Create: `WorkloadPlannerServiceTest.java`

**Interfaces:**

```java
public enum WorkloadPeriodicity { DAILY, WEEKLY, MONTHLY }
```

```java
public record WorkloadCalculation(
    long totalMinutes,
    List<WeekAllocation> weeks,
    List<DayAllocation> days
) {
    public record WeekAllocation(
        LocalDate weekStart,
        LocalDate weekEnd,
        int selectedDays,
        long allocatedMinutes,
        Set<DayOfWeek> inferredPattern
    ) {}
    public record DayAllocation(LocalDate date, long allocatedMinutes) {}
}
```

```java
public WorkloadCalculation calculate(
    YearMonth month,
    WorkloadPeriodicity periodicity,
    long enteredDurationMinutes,
    Set<LocalDate> plannedDates
)
```

- [ ] **Step 1: Write failing workload service tests**

Cover at least:

```java
@Test void dailyMultipliesEachSelectedDate() { /* 8h30 × 20 = 10200 min */ }
@Test void monthlyDistributesRemainderChronologically() { /* 10 min / 3 = 4,3,3 */ }
@Test void weeklyGivesFullLoadToEachNonEmptyInternalWeek() { /* 4,5,6 days */ }
@Test void weeklyProratesBoundaryByMostFrequentExactPattern() { /* 2/5 */ }
@Test void weeklyTieUsesNearestInternalWeekToEachBoundary() { /* deterministic */ }
@Test void weeklyBoundaryWithoutEvidenceUsesFiveDays() { /* 40h × 2/5 */ }
@Test void weeklyCapsBoundaryAtOneFullWeek() { /* 6 selected, baseline 5 */ }
@Test void halfMinuteRoundsUpOnceAtWeekLevel() { /* no per-day drift */ }
@Test void rejectsSundayOutOfMonthNegativeAndZeroDuration() { /* explicit messages */ }
```

- [ ] **Step 2: Run and confirm red state**

```bash
./mvnw -q -Dtest=WorkloadPlannerServiceTest test
```

Expected: FAIL because types do not exist.

- [ ] **Step 3: Implement the workload service**

Algorithm requirements:

1. validate month, mode, positive minutes and all dates;
2. deduplicate and sort dates;
3. group by Monday week start;
4. derive full month boundaries and classify first/last groups as boundary only when the calendar week crosses the month;
5. for each non-empty internal week, store the exact weekday set;
6. infer a boundary pattern by frequency; tie by nearest candidate week, then earlier week;
7. use five-day fallback only when no internal pattern exists;
8. calculate boundary minutes with integer numerator and denominator, rounding half-up once;
9. distribute each week/month total with quotient and chronological remainder;
10. return immutable result lists whose day sum equals `totalMinutes`.

- [ ] **Step 4: Run workload unit tests**

```bash
./mvnw -q -Dtest=WorkloadPlannerServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/goal/domain/WorkloadPeriodicity.java \
        src/main/java/dev/harrison/rendacomcarro/goal/application/WorkloadCalculation.java \
        src/main/java/dev/harrison/rendacomcarro/goal/application/WorkloadPlannerService.java \
        src/test/java/dev/harrison/rendacomcarro/goal/WorkloadPlannerServiceTest.java
git commit -m "feat: calculate monthly workload from planned dates"
```

---

### Task 5: Persist source workload and migrate existing goals

**Files:**
- Create: `V9__add_goal_workload_planner.sql`
- Modify: `MonthlyGoal.java`, `PlannedWorkDay.java`, `GoalService.java`, `GoalForm.java`, `GoalFormSubmissionService.java`, `GoalController.java`
- Modify goal service/submission/web tests.

**Interfaces:**
- `GoalForm` fields: `WorkloadPeriodicity workloadPeriodicity`, `Long workloadHours`, `Integer workloadMinutes`, plus existing month/goals/dates.
- `GoalForm.enteredDurationMinutes() -> long`.
- `MonthlyGoal.create(..., WorkloadPeriodicity periodicity, long enteredMinutes, long calculatedMinutes)`.
- `GoalService.create(..., WorkloadPeriodicity periodicity, long enteredMinutes, Set<LocalDate> dates)` delegates to `WorkloadPlannerService`.

- [ ] **Step 1: Write failing persistence and migration tests**

Update tests to assert:

- weekly source value persists as `WEEKLY + 2400`;
- calculated minutes persist independently;
- `plannedHours` equals `calculatedMinutes / 60` with scale 2;
- `PlannedWorkDay.plannedHours` reflects day allocations instead of zero;
- editing/read models expose original mode and duration;
- legacy migration maps old `planned_hours` to `MONTHLY` and rounded minutes.

- [ ] **Step 2: Run targeted tests and confirm failure**

```bash
./mvnw -q -Dtest=GoalServiceTest,GoalFormSubmissionServiceTest,GoalWebTest test
```

Expected: FAIL due missing fields/signatures.

- [ ] **Step 3: Add Flyway migration**

Create:

```sql
ALTER TABLE monthly_goal ADD COLUMN workload_periodicity VARCHAR(16);
ALTER TABLE monthly_goal ADD COLUMN entered_duration_minutes BIGINT;
ALTER TABLE monthly_goal ADD COLUMN calculated_month_minutes BIGINT;

UPDATE monthly_goal
SET workload_periodicity = 'MONTHLY',
    entered_duration_minutes = ROUND(planned_hours * 60),
    calculated_month_minutes = ROUND(planned_hours * 60);

ALTER TABLE monthly_goal ALTER COLUMN workload_periodicity SET NOT NULL;
ALTER TABLE monthly_goal ALTER COLUMN entered_duration_minutes SET NOT NULL;
ALTER TABLE monthly_goal ALTER COLUMN calculated_month_minutes SET NOT NULL;

ALTER TABLE monthly_goal ADD CONSTRAINT ck_monthly_goal_workload_periodicity
  CHECK (workload_periodicity IN ('DAILY', 'WEEKLY', 'MONTHLY'));
ALTER TABLE monthly_goal ADD CONSTRAINT ck_monthly_goal_entered_minutes
  CHECK (entered_duration_minutes > 0);
ALTER TABLE monthly_goal ADD CONSTRAINT ck_monthly_goal_calculated_minutes
  CHECK (calculated_month_minutes > 0);
```

- [ ] **Step 4: Update entity and service flow**

Persist source mode, entered minutes and calculated minutes. Continue writing `planned_hours` only as a derived compatibility value. Save each `PlannedWorkDay` using its allocated minutes converted to scale-2 hours.

- [ ] **Step 5: Update form/controller validation**

`GoalForm` must require mode/hours/minutes for final submission, allow zero hours when minutes are positive, reject minutes outside `0..59`, and preserve the existing planned-date validation. `GoalController` maps workload errors to the workload fields and planned-date errors to `plannedDates`.

- [ ] **Step 6: Run targeted Java tests**

```bash
./mvnw -q -Dtest=WorkloadPlannerServiceTest,GoalServiceTest,GoalFormSubmissionServiceTest,GoalWebTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/db/migration/V9__add_goal_workload_planner.sql \
        src/main/java/dev/harrison/rendacomcarro/goal/domain/MonthlyGoal.java \
        src/main/java/dev/harrison/rendacomcarro/goal/domain/PlannedWorkDay.java \
        src/main/java/dev/harrison/rendacomcarro/goal/application/GoalService.java \
        src/main/java/dev/harrison/rendacomcarro/goal/application/GoalFormSubmissionService.java \
        src/main/java/dev/harrison/rendacomcarro/goal/web/GoalForm.java \
        src/main/java/dev/harrison/rendacomcarro/goal/web/GoalController.java \
        src/test/java/dev/harrison/rendacomcarro/goal/GoalServiceTest.java \
        src/test/java/dev/harrison/rendacomcarro/goal/GoalFormSubmissionServiceTest.java \
        src/test/java/dev/harrison/rendacomcarro/goal/GoalWebTest.java
git commit -m "feat: persist workload source and calculated minutes"
```

---

### Task 6: Draft schema for workload source fields

**Files:**
- Modify: `MonthlyGoalDraftDefinition.java`
- Modify: `FormDraftDefinitionTest.java`
- Modify: `GoalFormSubmissionServiceTest.java`

**Interfaces:**
- Allowed draft fields become `month`, `personalNetGoal`, `operationalGoal`, `workloadPeriodicity`, `workloadHours`, `workloadMinutes`, `plannedDates`.
- Schema version becomes `2` for monthly goals.

- [ ] **Step 1: Write failing draft normalization tests**

Verify:

- partial draft can save mode/duration without dates;
- step-2 validation requires positive combined duration and dates;
- minutes `60` are rejected;
- unknown old client-derived totals are rejected;
- restored payload preserves `WEEKLY`, `40`, `0` exactly.

- [ ] **Step 2: Run and confirm failure**

```bash
./mvnw -q -Dtest=FormDraftDefinitionTest,GoalFormSubmissionServiceTest test
```

Expected: FAIL under schema 1 and old fields.

- [ ] **Step 3: Implement schema 2 normalization**

Normalize mode to enum name, hours to non-negative integer, minutes to integer `0..59`, and only require dates when validating step 2. Do not store calculated preview totals.

- [ ] **Step 4: Run draft tests**

```bash
./mvnw -q -Dtest=FormDraftDefinitionTest,GoalFormSubmissionServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/draft/application/definition/MonthlyGoalDraftDefinition.java \
        src/test/java/dev/harrison/rendacomcarro/draft/FormDraftDefinitionTest.java \
        src/test/java/dev/harrison/rendacomcarro/goal/GoalFormSubmissionServiceTest.java
git commit -m "feat: preserve workload source in goal drafts"
```

---

### Task 7: Pure JavaScript workload preview

**Files:**
- Create: `goal-workload-calculator.js`
- Create: `goal-workload-calculator.test.mjs`

**Interfaces:**

```js
calculateGoalWorkload({ month, periodicity, enteredMinutes, plannedDates })
// -> { status: 'ready'|'pending', totalMinutes, weeks, days }
```

- [ ] **Step 1: Write failing mirror-algorithm tests**

Use the same fixtures as `WorkloadPlannerServiceTest`, including exact expected minute totals and inferred weekday sets. Add an explicit contract fixture JSON in the test file and assert all three modes.

- [ ] **Step 2: Run and confirm red state**

```bash
node --test src/test/js/goal-workload-calculator.test.mjs
```

Expected: FAIL with `ERR_MODULE_NOT_FOUND`.

- [ ] **Step 3: Implement the pure calculator**

Mirror the Java rules exactly. Use UTC-safe date-string arithmetic rather than locale-dependent `Date` parsing for week grouping. Use integer arithmetic and half-up division helper:

```js
const divideHalfUp = (numerator, denominator) =>
  Math.floor((numerator * 2 + denominator) / (2 * denominator));
```

Return `pending` only when no planned dates are available; invalid values throw typed `Error` messages used by tests.

- [ ] **Step 4: Run JS calculator tests**

```bash
node --test src/test/js/goal-workload-calculator.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/js/goal-workload-calculator.js \
        src/test/js/goal-workload-calculator.test.mjs
git commit -m "feat: calculate workload preview in browser"
```

---

### Task 8: Goal workload UI and autosave integration

**Files:**
- Create: `goal-workload-planner.js`
- Create: `goal-workload-planner.test.mjs`
- Modify: `goals/form.html`, `goal-form.js`, `goals/detail.html`, `GoalWebTest.java`, `GuidedFormsWebContractTest.java`

**Interfaces:**
- `initializeGoalWorkloadPlanner(form) -> controller | null`
- Visible fields: `workloadPeriodicity`, `workloadHours`, `workloadMinutes`.
- Hidden/derived values are not authoritative and are not included in draft payload.

- [ ] **Step 1: Write failing DOM and rendered-HTML tests**

Assert the form renders:

- a three-option periodicity control;
- separate hour and minute fields;
- minute bounds `0..59`;
- an aria-live summary region;
- no editable `plannedHours` field;
- `data-draft-schema-version="2"`.

JavaScript tests must verify switching mode, no-date pending message, recalculation after planned-date changes, draft restoration, and one autosave event per real edit.

- [ ] **Step 2: Run targeted tests and confirm failure**

```bash
node --test src/test/js/goal-workload-planner.test.mjs
./mvnw -q -Dtest=GoalWebTest,GuidedFormsWebContractTest test
```

Expected: FAIL.

- [ ] **Step 3: Implement the goal workload component**

Use compact Bootstrap/Tabler controls rather than a clock picker. Render a summary containing original workload, per-week selected days/minutes, inferred boundary pattern, and total month duration. Format durations as `8 h`, `8 h 30 min`, or `30 min`.

`goal-form.js` remains responsible for date chips and draft context; it delegates workload rendering to `initializeGoalWorkloadPlanner` and includes source fields in the normal form payload.

- [ ] **Step 4: Update detail display**

Show original mode/duration and derived monthly total from the persisted goal. Existing `plannedHours` may remain visible as a compatibility total only where appropriate.

- [ ] **Step 5: Run JS and goal web tests**

```bash
npm run test:js
./mvnw -q -Dtest=GoalWebTest,GuidedFormsWebContractTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/js/goal-workload-planner.js \
        src/test/js/goal-workload-planner.test.mjs \
        src/main/resources/static/js/goal-form.js \
        src/main/resources/templates/goals/form.html \
        src/main/resources/templates/goals/detail.html \
        src/test/java/dev/harrison/rendacomcarro/goal/GoalWebTest.java \
        src/test/java/dev/harrison/rendacomcarro/web/GuidedFormsWebContractTest.java
git commit -m "feat: add daily weekly and monthly workload input"
```

---

### Task 9: Full regression, PR description and CI

**Files:**
- Modify: `docs/architecture.md` if workload persistence is documented there.
- Modify PR #9 title/body through GitHub metadata after code verification.

- [ ] **Step 1: Run all local test suites**

```bash
npm run test:js
./mvnw test
./mvnw -q -DskipTests package
```

Expected: all commands exit `0`.

- [ ] **Step 2: Review changed source for duplicated parsers and stale field names**

Run:

```bash
grep -R "function parseDecimal\|plannedHours\|vehicle-form-inputs" \
  src/main/resources/static/js src/main/java src/main/resources/templates src/test
```

Expected:

- no form-local `parseDecimal` functions;
- `plannedHours` only in compatibility/domain/reporting paths, not as editable source input;
- generic formatters imported from `localized-input-formatters.js`, not from the vehicle module.

- [ ] **Step 3: Inspect branch diff**

```bash
git status --short
git diff --check main...HEAD
git log --oneline --decorate -12
```

Expected: clean worktree, no whitespace errors, focused commits.

- [ ] **Step 4: Update PR #9 metadata**

Retitle to reflect both concerns, for example:

```text
feat: serialize guided saves and standardize guided inputs
```

Update the body with:

- original race-condition fix;
- shared localized masks;
- workload planner and migration;
- TDD evidence;
- final CI run number and commit SHA;
- explicit statement that merge is not automatic.

- [ ] **Step 5: Wait for GitHub Actions and inspect every job**

Required green checks:

- JavaScript tests;
- JUnit/MockMvc/Testcontainers;
- Maven package;
- credential scan;
- ARM64 image;
- Compose validation/startup;
- healthcheck;
- backup and isolated restore;
- clean shutdown.

- [ ] **Step 6: Final commit only if documentation changed**

```bash
git add docs/architecture.md
git commit -m "docs: describe goal workload persistence"
```

Do not create an empty commit.
