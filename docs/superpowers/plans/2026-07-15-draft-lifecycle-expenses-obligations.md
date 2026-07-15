# Draft Lifecycle for Expenses and Obligations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop empty and discarded drafts from being recreated, remove false conflict prompts, give expenses a non-blocking previous-draft flow, and enforce at most one active obligation draft per user.

**Architecture:** The shared guided-form controller will own dirty-state, submission, discard, and emergency-copy reconciliation. Expenses will use a unique session draft key plus an optional previous draft key. Obligations will use a database-enforced single-draft policy, a server-rendered decision page, and an idempotent discard/restart flow.

**Tech Stack:** Java 21, Spring Boot, Thymeleaf, JavaScript ES modules, PostgreSQL, Flyway, Maven, Node test runner, Bootstrap/Tabler.

## Global Constraints

- No new frontend framework or runtime dependency.
- Opening a form must not create a draft.
- Programmatic initialization must not mark a form dirty.
- Explicit discard is idempotent and must prevent `pagehide` from recreating the draft.
- False conflicts caused only by version, timestamp, or a completed keepalive request must be reconciled silently.
- Expense and obligation policies remain distinct.
- At most one persisted `OBLIGATION` draft exists per user.
- No merge or auto-merge.
- Full CI runs only after all implementation tasks are complete; failures are then investigated and corrected.

---

### Task 1: Shared draft state and regression tests

**Files:**
- Modify: `src/test/js/guided-form.test.mjs`
- Modify: `src/test/js/form-draft-client.test.mjs`
- Modify: `src/main/resources/static/js/guided-form-state.js`

**Interfaces:**
- Produces: `stableDraftPayload(form)`, `draftPayloadsEqual(left, right)` and dirty-state fixtures used by the controller.

- [ ] **Step 1: Add failing tests**

Cover these contracts:

```javascript
assert.equal(controller.dirty, false);
form.dispatchUserInput("notes", "changed");
assert.equal(controller.dirty, true);
form.dispatchUserInput("notes", "initial");
assert.equal(controller.dirty, false);
```

Also verify normalized payload equality ignores object key order and framework-only controls.

- [ ] **Step 2: Run the focused tests and confirm failure**

Run:

```bash
npm test -- --test-name-pattern="dirty|payload equality|pagehide"
```

Expected: failures because the lifecycle state and comparison helpers do not yet exist.

- [ ] **Step 3: Implement canonical payload comparison**

Add deterministic serialization helpers to `guided-form-state.js`. They must use `serializeEditableFields`, remove framework-only controls, sort object keys recursively, and compare resulting JSON strings.

- [ ] **Step 4: Run focused JavaScript tests**

Expected: payload helper tests pass; controller lifecycle tests remain red until Task 2.

- [ ] **Step 5: Commit**

```bash
git add src/test/js/guided-form.test.mjs src/test/js/form-draft-client.test.mjs src/main/resources/static/js/guided-form-state.js
git commit -m "test: define guided draft lifecycle contracts"
```

### Task 2: Shared guided-form lifecycle

**Files:**
- Modify: `src/main/resources/static/js/guided-form.js`
- Modify: `src/main/resources/static/js/form-draft-client.js`
- Modify: `src/main/resources/templates/fragments/guided-form.html`
- Test: `src/test/js/guided-form.test.mjs`
- Test: `src/test/js/form-draft-client.test.mjs`

**Interfaces:**
- Produces: `GuidedFormController.discardCurrentDraft()`, `markSubmitting()`, `setDraftIdentity(contextKey, version)`, and content-aware emergency reconciliation.

- [ ] **Step 1: Add failing controller tests**

Verify:

```javascript
await controller.connect();
assert.equal(client.saveCalls.length, 0);
controller.handlePagehide();
assert.equal(client.saveCalls.length, 0);
```

Verify discard waits for an active save, deletes once, clears emergency storage, marks the controller disposed, and ignores later `pagehide`.

Verify form submit marks `submitting`, cancels pending timers, and prevents a parallel autosave.

- [ ] **Step 2: Add failing reconciliation tests**

Cases:

```javascript
local.payload === server.payload // no modal, clear local emergency copy
local.payload !== server.payload // real conflict
```

The local timestamp shown in conflict data must come from emergency `savedAt`, not `new Date()`.

- [ ] **Step 3: Run focused tests and confirm failure**

```bash
npm test -- --test-name-pattern="discard|submitting|emergency|conflict|pagehide"
```

- [ ] **Step 4: Implement lifecycle flags**

Initialize and maintain:

```javascript
this.dirty = false;
this.discarding = false;
this.submitting = false;
this.disposed = false;
this.initialPayload = null;
this.lastPersistedPayload = null;
```

Capture the initial payload after form-specific initialization by waiting one animation frame and one microtask before binding dirty tracking. Input/change events with `event.isTrusted !== false` recalculate dirty state; programmatic restoration explicitly resets the baseline.

- [ ] **Step 5: Restrict save and pagehide**

`save()` returns without a request when the form is clean unless `immediate` is explicitly required for navigation. `pagehide` saves only when dirty and not discarding, submitting, or disposed.

- [ ] **Step 6: Implement definitive discard**

`discardCurrentDraft()` must clear timers, wait for `savePromise`, call the idempotent DELETE, clear emergency storage, reset dirty state, and mark the controller disposed before navigation.

- [ ] **Step 7: Implement content-aware reconciliation**

Extend emergency data with `savedAt` and a stable tab ID from `sessionStorage`. If server and local payloads are equivalent, adopt the server version and clear the emergency copy without showing a modal.

- [ ] **Step 8: Simplify shared dialogs**

Remove the automatic recovery modal dependency from the shared controller. Keep the conflict component only for real material conflicts and change its title to `Existem alterações diferentes neste rascunho`.

- [ ] **Step 9: Run all JavaScript tests**

```bash
npm run test:js
```

Expected: all JavaScript tests pass.

- [ ] **Step 10: Commit**

```bash
git add src/main/resources/static/js/guided-form.js src/main/resources/static/js/form-draft-client.js src/main/resources/templates/fragments/guided-form.html src/test/js
git commit -m "fix: make guided draft lifecycle explicit"
```

### Task 3: Draft API and owner-safe idempotent operations

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/draft/application/FormDraftService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/draft/infrastructure/FormDraftRepository.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/draft/web/FormDraftController.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/draft/FormDraftApiTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/draft/FormDraftServiceTest.java`

**Interfaces:**
- Produces: `listActive(username, type)`, `discard(username, type, contextKey)`, `completeAll(username, type, contextKeys)`, and repository methods scoped by owner and type.

- [ ] **Step 1: Add failing backend tests**

Cover:

```java
drafts.discard(username, type, key); // twice, no exception
assertThat(drafts.listActive(username, EXPENSE)).isEmpty();
```

Verify one user cannot list or delete another user's draft. Verify completing multiple keys is idempotent.

- [ ] **Step 2: Run focused Maven tests**

```bash
./mvnw -q -Dtest=FormDraftApiTest,FormDraftServiceTest test
```

Expected: failures for missing idempotent and multi-key APIs.

- [ ] **Step 3: Add repository operations**

Add owner/type scoped bulk delete and ordered list methods. All delete methods must include owner username.

- [ ] **Step 4: Implement service operations**

Remove version checking from explicit discard. Preserve optimistic version checking for save. Add multi-key completion that filters blank/duplicate keys before deletion.

- [ ] **Step 5: Generalize list endpoint**

Expose `GET /api/form-drafts/{type}/list` for authenticated users while preserving the response shape.

- [ ] **Step 6: Run focused tests**

Expected: all draft API/service tests pass.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/draft src/test/java/dev/harrison/rendacomcarro/draft
git commit -m "feat: add owner-safe draft lifecycle operations"
```

### Task 4: Expense previous-draft flow

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseController.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseForm.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/application/ExpenseFormSubmissionService.java`
- Modify: `src/main/resources/templates/expenses/form.html`
- Modify: `src/main/resources/static/js/expense-form.js`
- Modify: `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseWebTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseFormSubmissionServiceTest.java`
- Modify: `src/test/js/expense-form.test.mjs`

**Interfaces:**
- Produces hidden fields `draftContextKey` and `previousDraftContextKey`; produces non-modal previous-draft actions.

- [ ] **Step 1: Add failing web and submission tests**

Verify `/expenses/new` renders a unique `expense:new:<uuid>` key, does not auto-open a modal, and exposes a previous-draft region only when a prior draft exists.

Verify successful submission removes both keys and validation failure removes neither.

- [ ] **Step 2: Add failing JavaScript tests**

Verify `Continuar rascunho` switches the controller identity and restores the prior payload. Verify `Descartar rascunho` deletes only the previous key and leaves the current form unchanged.

- [ ] **Step 3: Run focused tests and confirm failure**

```bash
npm test -- --test-name-pattern="expense previous draft"
./mvnw -q -Dtest=ExpenseWebTest,ExpenseFormSubmissionServiceTest test
```

- [ ] **Step 4: Add form fields and server model**

Generate `expense:new:<uuid>` in the GET controller. Populate the most recent existing expense draft summary separately. Add `draftContextKey` and `previousDraftContextKey` to `ExpenseForm` with draft-ignore semantics.

- [ ] **Step 5: Replace recovery modal with inline banner**

Render title, saved timestamp, `Continuar rascunho`, and `Descartar rascunho`. Do not include the shared recovery modal for expenses.

- [ ] **Step 6: Implement client actions**

Use `FormDraftClient.load/discard` and controller identity methods. Confirm only when continuing would discard dirty current edits.

- [ ] **Step 7: Complete both keys after creation**

Pass both keys into the submission service and remove them in the same transaction after the expense is created.

- [ ] **Step 8: Run focused tests**

Expected: expense JavaScript and Java tests pass.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/expense src/main/resources/templates/expenses/form.html src/main/resources/static/js/expense-form.js src/test/java/dev/harrison/rendacomcarro/expense src/test/js/expense-form.test.mjs
git commit -m "fix: replace expense draft modal with inline recovery"
```

### Task 5: Enforce one obligation draft

**Files:**
- Create: `src/main/resources/db/migration/V12__enforce_single_obligation_draft.sql`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/application/ObligationDraftPolicyService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/draft/application/FormDraftService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/draft/infrastructure/FormDraftRepository.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/draft/web/FormDraftController.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/draft/ObligationDraftPolicyServiceTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/draft/FormDraftApiTest.java`

**Interfaces:**
- Produces: `Optional<DraftView> findActive(username)`, `discardActive(username)`, and an atomic single-draft save policy.

- [ ] **Step 1: Add failing migration/policy tests**

Create several obligation drafts for one owner with different `updatedAt` values. Assert only the newest survives normalization. Add a concurrency test proving two different keys cannot persist simultaneously.

- [ ] **Step 2: Run focused tests and confirm failure**

```bash
./mvnw -q -Dtest=ObligationDraftPolicyServiceTest,FormDraftApiTest test
```

- [ ] **Step 3: Add Flyway migration**

Use a window function to delete all but the newest `OBLIGATION` row per owner, then create:

```sql
CREATE UNIQUE INDEX ux_form_draft_single_obligation_owner
ON form_draft(owner_id)
WHERE form_type = 'OBLIGATION';
```

- [ ] **Step 4: Implement policy service**

Before creating a new obligation draft, remove expired rows for that owner/type. On unique-index violation, return a typed conflict containing the active draft summary rather than a generic other-device conflict.

- [ ] **Step 5: Add active/discard API operations**

Expose owner-scoped operations needed by the obligation decision screen. Responses return zero or one active item.

- [ ] **Step 6: Run focused tests**

Expected: migration, uniqueness, owner isolation, and idempotent discard pass.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/db/migration/V12__enforce_single_obligation_draft.sql src/main/java/dev/harrison/rendacomcarro/draft src/test/java/dev/harrison/rendacomcarro/draft
git commit -m "feat: enforce one obligation draft per user"
```

### Task 6: Obligation decision and discard UX

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/web/FinancialObligationController.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/application/ObligationFormSubmissionService.java`
- Create: `src/main/resources/templates/obligations/draft-decision.html`
- Modify: `src/main/resources/templates/obligations/form.html`
- Modify: `src/main/resources/templates/obligations/list.html`
- Modify: `src/main/resources/static/js/obligation-form.js`
- Modify: `src/test/java/dev/harrison/rendacomcarro/web/GuidedFormsWebContractTest.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/finance/ObligationDraftWebTest.java`
- Modify: `src/test/js/obligation-form.test.mjs`

**Interfaces:**
- Produces routes for continue, discard-and-start, and owner-safe draft opening.

- [ ] **Step 1: Add failing web tests**

Verify:

```text
GET /obligations/new + active draft -> decision page
GET /obligations/new?fresh=true + no active draft -> empty form
GET /obligations/new?draftKey=<active> -> restored form
```

An old/deleted key must not recreate a draft.

- [ ] **Step 2: Add failing template and JavaScript tests**

The list page shows at most one draft card with `Continuar` and `Descartar`. The form uses `Sair e manter rascunho` and a separate `Descartar rascunho` action. It must not include the generic recovery modal.

- [ ] **Step 3: Run focused tests and confirm failure**

```bash
npm test -- --test-name-pattern="obligation draft"
./mvnw -q -Dtest=ObligationDraftWebTest,GuidedFormsWebContractTest test
```

- [ ] **Step 4: Implement decision route and page**

When an active draft exists and no explicit valid `draftKey` is supplied, render the decision page with its saved timestamp and three clear actions.

- [ ] **Step 5: Implement discard-and-start**

Use POST with CSRF. Discard the active draft idempotently and redirect to `/obligations/new?fresh=true`. Opening the fresh form creates only a candidate key in HTML.

- [ ] **Step 6: Update form navigation**

`Sair e manter rascunho` requests an immediate save only when dirty, waits for completion, then navigates. `Descartar rascunho` uses the definitive shared discard path.

- [ ] **Step 7: Remove all obligation drafts on success**

After creating the obligation, delete the active obligation draft for that user in the same transaction.

- [ ] **Step 8: Run focused tests**

Expected: all obligation draft tests pass.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/finance src/main/resources/templates/obligations src/main/resources/static/js/obligation-form.js src/test/java/dev/harrison/rendacomcarro/finance src/test/java/dev/harrison/rendacomcarro/web src/test/js/obligation-form.test.mjs
git commit -m "fix: give obligation drafts a single clear lifecycle"
```

### Task 7: Full regression and CI correction loop

**Files:**
- Modify only files proven necessary by failing tests.
- Update: `docs/superpowers/specs/2026-07-14-draft-lifecycle-and-expense-deletion-design.md` only when implementation reveals a real contradiction.

- [ ] **Step 1: Run all JavaScript tests locally**

```bash
npm run test:js
```

Expected: zero failures.

- [ ] **Step 2: Run focused Java tests when local infrastructure permits**

```bash
./mvnw test
```

Expected: zero failures. When local dependency download or database access is unavailable, record that limitation and rely on CI for the complete Java execution.

- [ ] **Step 3: Review the final diff**

Check specifically for:

```text
recovery-modal still included by expense/obligation templates
pagehide save without dirty guard
DELETE requiring a stale version
obligation list rendering more than one active draft
old key reopening a removed obligation draft
```

- [ ] **Step 4: Push the complete head and wait for CI**

Do not stop between tasks for intermediate CI. After the full implementation is present, inspect every CI job.

- [ ] **Step 5: Debug any CI failure systematically**

Use reports/logs to identify root cause. Add or update a regression test before changing production code when the failure represents missing behavior.

- [ ] **Step 6: Re-run CI until the complete workflow succeeds**

Verify JavaScript, Maven tests, packaging, credentials, Compose validation, ARM64 image, stack healthcheck, backup, and restore.

- [ ] **Step 7: Update the PR description**

Record the verified head, workflow ID, corrected lifecycle behavior, and explicitly state that no merge or auto-merge occurred.
