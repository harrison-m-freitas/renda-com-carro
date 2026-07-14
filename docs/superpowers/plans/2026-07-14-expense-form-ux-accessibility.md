# Expense Form UX and Accessibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the new-expense flow with user-time-zone dates, explicit payment state, accessible classification cards, safe localized inputs, explainable mileage allocation previews, schema-2 drafts, and authoritative backend validation.

**Architecture:** Add a small global user-time-zone preference boundary, then keep expense calculations in a pure JavaScript state module and domain validation in Java. A read-only allocation-preview service reuses persisted monthly closings first and `MonthlyMileageInferenceService` second; the browser only renders previews and never persists derived allocation values.

**Tech Stack:** Java 21, Spring Boot, Spring MVC, Spring Security, Spring Data JPA, Flyway, Thymeleaf, ES modules, Node test runner, Bootstrap/Tabler-compatible CSS, PostgreSQL, Testcontainers.

## Global Constraints

- Do not merge automatically into `main`.
- Use TDD: every production behavior starts with a failing automated test.
- Preserve the three guided steps and existing autosave queue semantics.
- Keep the backend authoritative; never trust professional/personal values derived in the browser.
- Store time zones as IANA IDs and require confirmation before changing an existing preference.
- Do not change the dashboard accounting policy for months without a confirmed closing.
- Preserve schema-1 expense drafts through deterministic schema-2 normalization.
- Run JavaScript tests, Java/MockMvc/Testcontainers tests, package verification, and complete CI before claiming completion.

---

## File Map

### New files

- `src/main/resources/db/migration/V11__add_user_time_zone.sql`: nullable IANA time-zone preference.
- `src/main/java/dev/harrison/rendacomcarro/security/application/UserTimeZoneService.java`: validates, resolves, and updates the authenticated user's time zone.
- `src/main/java/dev/harrison/rendacomcarro/security/web/UserTimeZoneController.java`: authenticated JSON read/update API.
- `src/main/java/dev/harrison/rendacomcarro/security/web/GlobalUserPreferencesAdvice.java`: exposes saved/default time-zone metadata to authenticated layouts.
- `src/main/resources/static/js/user-time-zone.js`: device detection, divergence confirmation, local decision memory, and global change event.
- `src/main/java/dev/harrison/rendacomcarro/expense/application/ExpenseAllocationPreviewService.java`: chooses confirmed closing, existing inference, or insufficient data.
- `src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseAllocationPreviewResponse.java`: stable JSON contract.
- `src/main/resources/static/js/expense-form-state.js`: pure date, payment, validation, integer-money, allocation, and summary state.
- `src/test/js/user-time-zone.test.mjs`: browser-independent time-zone behavior tests.
- `src/test/js/expense-form-state.test.mjs`: pure expense-state tests.
- `src/test/js/expense-form.test.mjs`: DOM integration tests.
- `src/test/java/dev/harrison/rendacomcarro/security/UserTimeZoneServiceTest.java`: preference rules and date resolution.
- `src/test/java/dev/harrison/rendacomcarro/security/UserTimeZoneWebTest.java`: authenticated API and layout contract.
- `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseAllocationPreviewServiceTest.java`: confirmed/estimated/insufficient decisions.

### Modified files

- `src/main/java/dev/harrison/rendacomcarro/security/domain/AppUser.java`
- `src/main/java/dev/harrison/rendacomcarro/security/application/CurrentUserService.java`
- `src/main/resources/templates/layouts/app.html`
- `src/main/resources/templates/fragments/head.html`
- `src/main/resources/static/css/app.css`
- `src/main/resources/static/js/localized-input-formatters.js`
- `src/main/resources/static/js/localized-inputs.js`
- `src/test/js/localized-input-formatters.test.mjs`
- `src/test/js/localized-inputs.test.mjs`
- `src/main/java/dev/harrison/rendacomcarro/vehicle/application/VehicleService.java`
- `src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseForm.java`
- `src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseController.java`
- `src/main/java/dev/harrison/rendacomcarro/expense/application/ExpenseFormSubmissionService.java`
- `src/main/java/dev/harrison/rendacomcarro/expense/application/ExpenseService.java`
- `src/main/java/dev/harrison/rendacomcarro/draft/application/definition/ExpenseDraftDefinition.java`
- `src/main/resources/templates/expenses/form.html`
- `src/main/resources/static/js/expense-form.js`
- `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseWebTest.java`
- `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseFormSubmissionServiceTest.java`
- `src/test/java/dev/harrison/rendacomcarro/draft/FormDraftDefinitionTest.java`
- `src/test/java/dev/harrison/rendacomcarro/draft/FormDraftApiTest.java`
- `src/test/java/dev/harrison/rendacomcarro/web/GuidedFormsWebContractTest.java`

---

### Task 1: Persist and Resolve User Time Zones

**Files:**
- Create: `src/main/resources/db/migration/V11__add_user_time_zone.sql`
- Create: `src/main/java/dev/harrison/rendacomcarro/security/application/UserTimeZoneService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/security/domain/AppUser.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/security/application/CurrentUserService.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/security/UserTimeZoneServiceTest.java`

**Interfaces:**
- Produces: `ZoneId UserTimeZoneService.resolve(String username)`
- Produces: `LocalDate UserTimeZoneService.today(String username)`
- Produces: `AppUser UserTimeZoneService.update(String username, String zoneId)`
- Produces: `String AppUser.getTimeZoneId()` and `void AppUser.updateTimeZone(String zoneId)`

- [ ] **Step 1: Write failing service tests**

```java
@Test
void todayUsesSavedZoneInsteadOfServerZone() {
    user.updateTimeZone("Pacific/Kiritimati");
    when(clock.instant()).thenReturn(Instant.parse("2026-07-14T10:30:00Z"));
    assertThat(service.today(user.getUsername())).isEqualTo(LocalDate.of(2026, 7, 15));
}

@Test
void invalidZoneIsRejectedWithoutChangingPreference() {
    assertThatThrownBy(() -> service.update(user.getUsername(), "Mars/Olympus"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Fuso horário inválido.");
    assertThat(user.getTimeZoneId()).isNull();
}
```

- [ ] **Step 2: Run the focused tests and verify RED**

Run: `./mvnw -Dtest=UserTimeZoneServiceTest test`
Expected: compilation failure because `UserTimeZoneService` and `AppUser.updateTimeZone` do not exist.

- [ ] **Step 3: Add migration, entity field, and minimal service**

```sql
ALTER TABLE app_user ADD COLUMN time_zone_id VARCHAR(80);
```

```java
public ZoneId resolve(String username) {
    String saved = currentUsers.require(username).getTimeZoneId();
    return saved == null ? applicationClock.getZone() : ZoneId.of(saved);
}

public LocalDate today(String username) {
    return LocalDate.now(applicationClock.withZone(resolve(username)));
}

@Transactional
public AppUser update(String username, String zoneId) {
    ZoneId validated;
    try { validated = ZoneId.of(zoneId); }
    catch (DateTimeException exception) { throw new IllegalArgumentException("Fuso horário inválido."); }
    AppUser user = currentUsers.require(username);
    user.updateTimeZone(validated.getId());
    return user;
}
```

- [ ] **Step 4: Run focused service tests and verify GREEN**

Run: `./mvnw -Dtest=UserTimeZoneServiceTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V11__add_user_time_zone.sql \
  src/main/java/dev/harrison/rendacomcarro/security \
  src/test/java/dev/harrison/rendacomcarro/security/UserTimeZoneServiceTest.java
git commit -m "feat: persist user time zone preference"
```

### Task 2: Detect and Confirm Device Time-Zone Divergence

**Files:**
- Create: `src/main/java/dev/harrison/rendacomcarro/security/web/UserTimeZoneController.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/security/web/GlobalUserPreferencesAdvice.java`
- Create: `src/main/resources/static/js/user-time-zone.js`
- Create: `src/test/js/user-time-zone.test.mjs`
- Create: `src/test/java/dev/harrison/rendacomcarro/security/UserTimeZoneWebTest.java`
- Modify: `src/main/resources/templates/layouts/app.html`
- Modify: `src/main/resources/templates/fragments/head.html`
- Modify: `src/main/resources/static/css/app.css`

**Interfaces:**
- Produces: `GET /api/user-preferences/time-zone`
- Produces: `PUT /api/user-preferences/time-zone` with `{ "timeZoneId": "America/Sao_Paulo" }`
- Produces browser event: `app:time-zone-changed` with `{ timeZoneId }`
- Stores declined pair under `renda-com-carro:time-zone-decision:<saved>|<detected>`.

- [ ] **Step 1: Write failing JavaScript decision tests**

```javascript
test('same saved and detected zone does not prompt', () => {
  assert.equal(shouldPromptForTimeZone('America/Sao_Paulo', 'America/Sao_Paulo', new Map()), false);
});

test('declined saved and detected pair is remembered only for that pair', () => {
  const storage = new Map([['America/Sao_Paulo|America/New_York', 'keep']]);
  assert.equal(shouldPromptForTimeZone('America/Sao_Paulo', 'America/New_York', storage), false);
  assert.equal(shouldPromptForTimeZone('America/Sao_Paulo', 'Europe/Lisbon', storage), true);
});
```

- [ ] **Step 2: Run JavaScript tests and verify RED**

Run: `node --test src/test/js/user-time-zone.test.mjs`
Expected: module-not-found failure.

- [ ] **Step 3: Implement pure helpers, authenticated API, banner, and confirmation actions**

```javascript
export function decisionKey(saved, detected) {
  return `${saved ?? ''}|${detected ?? ''}`;
}

export function shouldPromptForTimeZone(saved, detected, storage) {
  if (!saved || !detected || saved === detected) return false;
  return storage.get(decisionKey(saved, detected)) !== 'keep';
}
```

The layout must expose saved/default zones as `data-*`, keep saved zone active until confirmation, and include two real buttons named `Usar fuso deste dispositivo` and `Manter fuso configurado`.

- [ ] **Step 4: Add failing MockMvc tests, then implement API and layout model**

Run: `./mvnw -Dtest=UserTimeZoneWebTest test`
Expected before implementation: 404 or missing layout data. Expected after implementation: PASS.

- [ ] **Step 5: Run JavaScript and focused Java tests**

Run: `node --test src/test/js/user-time-zone.test.mjs`
Run: `./mvnw -Dtest=UserTimeZoneWebTest,UserTimeZoneServiceTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/security/web \
  src/main/resources/templates/layouts/app.html src/main/resources/templates/fragments/head.html \
  src/main/resources/static/js/user-time-zone.js src/main/resources/static/css/app.css \
  src/test/js/user-time-zone.test.mjs src/test/java/dev/harrison/rendacomcarro/security/UserTimeZoneWebTest.java
git commit -m "feat: confirm device time zone changes"
```

### Task 3: Add Natural Percentage Input and Pure Expense State

**Files:**
- Create: `src/main/resources/static/js/expense-form-state.js`
- Create: `src/test/js/expense-form-state.test.mjs`
- Modify: `src/main/resources/static/js/localized-input-formatters.js`
- Modify: `src/main/resources/static/js/localized-inputs.js`
- Modify: `src/test/js/localized-input-formatters.test.mjs`
- Modify: `src/test/js/localized-inputs.test.mjs`

**Interfaces:**
- Produces: `formatNaturalPercentage(value)` and `normalizeNaturalPercentage(value)`.
- Produces: `createExpenseState(initial)`, `updateExpenseState(state, action)`, `validateExpenseState(state)`, `expenseDraftPayload(state)`, and `expenseSummary(state, allocationPreview)`.
- Money is represented as integer cents in summary calculations.

- [ ] **Step 1: Write failing formatter and state tests**

```javascript
test('natural percentage preserves whole percentage semantics', () => {
  assert.equal(formatNaturalPercentage('75'), '75');
  assert.equal(formatNaturalPercentage('75,5'), '75,50');
  assert.equal(formatNaturalPercentage('75.5'), '75,50');
});

test('mixed percentage rejects zero and one hundred', () => {
  assert.equal(validateExpenseState(mixedPercentage('0')).professionalPercentagePercent,
    'Para 0%, classifique o gasto como Pessoal.');
  assert.equal(validateExpenseState(mixedPercentage('100')).professionalPercentagePercent,
    'Para 100%, classifique o gasto como Profissional.');
});

test('paid date follows expense date until manually edited', () => {
  let state = createExpenseState({ expenseDate: '2026-07-14', paymentStatus: 'PAID' });
  state = updateExpenseState(state, { type: 'EXPENSE_DATE_CHANGED', value: '2026-07-15' });
  assert.equal(state.paidDate, '2026-07-15');
  state = updateExpenseState(state, { type: 'PAID_DATE_CHANGED', value: '2026-07-10' });
  state = updateExpenseState(state, { type: 'EXPENSE_DATE_CHANGED', value: '2026-07-16' });
  assert.equal(state.paidDate, '2026-07-10');
});
```

- [ ] **Step 2: Run focused JavaScript tests and verify RED**

Run: `node --test src/test/js/expense-form-state.test.mjs src/test/js/localized-input-formatters.test.mjs`
Expected: missing exports/module failure.

- [ ] **Step 3: Implement minimal formatter and pure reducer/state functions**

Use decimal-string parsing, integer cents, explicit error objects, and no `Math.min`/`Math.max` correction of invalid manual values.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run: `node --test src/test/js/expense-form-state.test.mjs src/test/js/localized-input-formatters.test.mjs src/test/js/localized-inputs.test.mjs`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/js/expense-form-state.js \
  src/main/resources/static/js/localized-input-formatters.js \
  src/main/resources/static/js/localized-inputs.js \
  src/test/js/expense-form-state.test.mjs \
  src/test/js/localized-input-formatters.test.mjs src/test/js/localized-inputs.test.mjs
git commit -m "feat: add safe expense form state"
```

### Task 4: Enforce Expense Rules and Active References in the Backend

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/vehicle/application/VehicleService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseForm.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/application/ExpenseFormSubmissionService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/application/ExpenseService.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseFormSubmissionServiceTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseWebTest.java`

**Interfaces:**
- Adds enum-like form property `paymentStatus` with values `PAID` and `PENDING`.
- Produces `Vehicle VehicleService.getActive(UUID id)` and `List<Vehicle> listActive()`.
- `PENDING` always submits `paidDate = null`; `PAID` requires `paidDate`.

- [ ] **Step 1: Write failing backend tests**

Add tests proving archived vehicles and inactive categories are rejected, pending clears a forged paid date, paid requires a date, manual percentages require `0 < x < 100`, fixed amount requires `0 < x < total`, and incompatible values are ignored.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `./mvnw -Dtest=ExpenseFormSubmissionServiceTest,ExpenseWebTest test`
Expected: FAIL on accepted invalid combinations or missing payment status.

- [ ] **Step 3: Implement minimal validation and normalization**

```java
LocalDate effectivePaidDate = form.getPaymentStatus() == PaymentStatus.PENDING
    ? null
    : requirePaidDate(form.getPaidDate());
```

Validate active vehicle/category before entity creation and keep contextual day/shift ownership checks.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run: `./mvnw -Dtest=ExpenseFormSubmissionServiceTest,ExpenseWebTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/vehicle/application/VehicleService.java \
  src/main/java/dev/harrison/rendacomcarro/expense \
  src/test/java/dev/harrison/rendacomcarro/expense
git commit -m "feat: enforce expense submission rules"
```

### Task 5: Reuse Monthly Mileage Inference for Allocation Preview

**Files:**
- Create: `src/main/java/dev/harrison/rendacomcarro/expense/application/ExpenseAllocationPreviewService.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseAllocationPreviewResponse.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseAllocationPreviewServiceTest.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseController.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseWebTest.java`

**Interfaces:**
- Produces statuses `CONFIRMED`, `ESTIMATED`, `INSUFFICIENT_DATA`.
- Produces `ExpenseAllocationPreviewService.preview(UUID vehicleId, YearMonth month)`.
- Produces authenticated `GET /expenses/allocation-preview?vehicleId=<uuid>&competenceMonth=yyyy-MM`.

- [ ] **Step 1: Write failing service tests for all three statuses**

A confirmed closing must win over inference. An inference with positive total and no blocking alert returns `ESTIMATED`. Zero total or blocking alerts returns `INSUFFICIENT_DATA` without a percentage.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `./mvnw -Dtest=ExpenseAllocationPreviewServiceTest test`
Expected: compilation failure because service/response do not exist.

- [ ] **Step 3: Implement the read-only service and response**

Do not persist any closing. Convert the stored ratio to display percentage only in the response. Preserve stable alert codes and Portuguese messages.

- [ ] **Step 4: Write failing endpoint test, implement endpoint, verify GREEN**

Run: `./mvnw -Dtest=ExpenseAllocationPreviewServiceTest,ExpenseWebTest test`
Expected after implementation: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/expense/application/ExpenseAllocationPreviewService.java \
  src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseAllocationPreviewResponse.java \
  src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseController.java \
  src/test/java/dev/harrison/rendacomcarro/expense
git commit -m "feat: expose expense allocation preview"
```

### Task 6: Upgrade Expense Drafts to Schema 2

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/draft/application/definition/ExpenseDraftDefinition.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/draft/FormDraftDefinitionTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/draft/FormDraftApiTest.java`

**Interfaces:**
- Schema version becomes `2`.
- Accepts `paymentStatus`, `operationalDayId`, and `shiftId`.
- Schema-1 logical normalization derives `PAID` from non-empty `paidDate`, otherwise `PENDING`.

- [ ] **Step 1: Write failing draft tests**

Test schema version 2, accepted context IDs, open interval validation, removal of incompatible values, pending removal of `paidDate`, and legacy payload normalization.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `./mvnw -Dtest=FormDraftDefinitionTest,FormDraftApiTest test`
Expected: FAIL because schema remains 1 and fields are rejected.

- [ ] **Step 3: Implement schema-2 normalization**

Require the same manual allocation boundaries as final submission. Never accept derived professional/personal amounts or preview status.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run: `./mvnw -Dtest=FormDraftDefinitionTest,FormDraftApiTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/draft/application/definition/ExpenseDraftDefinition.java \
  src/test/java/dev/harrison/rendacomcarro/draft/FormDraftDefinitionTest.java \
  src/test/java/dev/harrison/rendacomcarro/draft/FormDraftApiTest.java
git commit -m "feat: upgrade expense drafts to schema two"
```

### Task 7: Rebuild the Accessible Expense Form and Browser Automation

**Files:**
- Modify: `src/main/resources/templates/expenses/form.html`
- Modify: `src/main/resources/static/js/expense-form.js`
- Create: `src/test/js/expense-form.test.mjs`
- Modify: `src/main/resources/static/css/app.css`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseController.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseWebTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/web/GuidedFormsWebContractTest.java`

**Interfaces:**
- Consumes global `app:time-zone-changed`.
- Consumes `expense-form-state.js` and `/expenses/allocation-preview`.
- Preserves guided-form `before-save` and `restored` events.

- [ ] **Step 1: Write failing DOM and template-contract tests**

Assert real radio groups in `fieldset`/`legend`, professional/payment paid/mileage defaults, `details` for `Mês de referência`, error summary focus target, separate `role=status`, schema 2, active vehicle selection, and no classification `<select>`.

- [ ] **Step 2: Run JavaScript and focused web tests and verify RED**

Run: `node --test src/test/js/expense-form.test.mjs`
Run: `./mvnw -Dtest=ExpenseWebTest,GuidedFormsWebContractTest test`
Expected: FAIL against current template/script.

- [ ] **Step 3: Implement template and controller defaults**

Use user-zone `today`, active vehicles only, primary active vehicle when no restored/bound value, payment radios, classification cards, allocation radios, a mileage preview region, accessible server error summary, and expanded review rows.

- [ ] **Step 4: Implement browser integration**

The script must preserve transient manual values, synchronize untouched dates/month, abort stale preview requests, mark invalid controls with `aria-invalid`, block invalid guided advancement, strip incompatible draft fields, disable duplicate submission, and restore submit state on `pageshow`.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run: `node --test src/test/js/expense-form-state.test.mjs src/test/js/expense-form.test.mjs src/test/js/user-time-zone.test.mjs`
Run: `./mvnw -Dtest=ExpenseWebTest,GuidedFormsWebContractTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/templates/expenses/form.html \
  src/main/resources/static/js/expense-form.js src/main/resources/static/css/app.css \
  src/test/js/expense-form.test.mjs \
  src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseController.java \
  src/test/java/dev/harrison/rendacomcarro/expense/ExpenseWebTest.java \
  src/test/java/dev/harrison/rendacomcarro/web/GuidedFormsWebContractTest.java
git commit -m "feat: rebuild accessible expense form"
```

### Task 8: Full Regression Verification and Delivery

**Files:**
- Modify only when a failing regression test exposes a defect in this feature.

- [ ] **Step 1: Run all JavaScript tests**

Run: `npm run test:js`
Expected: all tests pass with zero failures.

- [ ] **Step 2: Run the complete Java suite**

Run: `./mvnw test`
Expected: all unit, MockMvc, and Testcontainers tests pass.

- [ ] **Step 3: Verify packaging**

Run: `./mvnw -DskipTests package`
Expected: `BUILD SUCCESS` and executable artifact under `target/`.

- [ ] **Step 4: Run repository verification commands mirrored by CI**

Run: `bash -n infra/backup/backup.sh infra/backup/restore.sh`
Run: `docker compose config --quiet`
Expected: zero exit status.

- [ ] **Step 5: Review diff for scope and secrets**

Run: `git diff main...HEAD --check`
Run: `git status --short`
Run: `git log --oneline --decorate main..HEAD`
Expected: no whitespace errors, clean worktree, only expense/time-zone scope commits.

- [ ] **Step 6: Push branch and open a non-draft PR without merging**

Branch: `feat/expense-form-ux-accessibility`
Title: `feat: improve expense form UX and time-zone handling`
The PR body must list tests actually executed and explicitly identify any verification performed only by GitHub Actions.
