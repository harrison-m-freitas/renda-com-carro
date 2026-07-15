# PR #15 P0/P1 Blocker Corrections Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Merge the current `main` into PR #15, preserve the single-obligation-draft lifecycle, fix the reviewed P1 financial defects, and return the draft PR to a conflict-free fully validated state.

**Architecture:** Resolve integration conflicts by keeping `main` as the source of truth for draft lifecycle, user preferences, and unrelated modules while layering the obligation redesign onto those contracts. Keep calculations authoritative in Java, mirror them in pure JavaScript for previews, and represent incomplete purchase-plan totals explicitly instead of inventing zero cost.

**Tech Stack:** Java 21, Spring Boot, Spring MVC, Spring Data JPA, Flyway, Thymeleaf, ES modules, Node test runner, PostgreSQL, Testcontainers, Maven, Docker Compose, GitHub Actions.

## Global Constraints

- Keep PR #15 in draft state.
- Do not merge or enable auto-merge into `main`.
- Merge the current `main` into `feat/obligation-financing-redesign` with a merge commit.
- Use TDD for every P1 behavior.
- Preserve all unrelated changes already merged into `main`.
- Restrict this cycle to P0 and P1 findings.
- Run the complete CI on the final head before claiming completion.

---

### Task 1: Merge Current Main and Resolve Structural Conflicts

**Files:**
- Modify conflicts under `src/main/java/dev/harrison/rendacomcarro/draft/**`
- Modify conflicts under `src/main/java/dev/harrison/rendacomcarro/finance/**`
- Modify conflicts under `src/main/resources/templates/obligations/**`
- Modify conflicts under `src/main/resources/static/js/guided-form*.js`
- Rename: `src/main/resources/db/migration/V11__redesign_financial_obligations.sql` → `src/main/resources/db/migration/V13__redesign_financial_obligations.sql`

**Interfaces:**
- Consumes: `FormDraftService.findLatestActive`, `discard`, optimistic conflict handling, and the unique obligation-draft database rule from `main`.
- Produces: a merge commit whose tree contains all current `main` changes plus the obligation redesign.

- [ ] **Step 1: Merge `origin/main` without committing**

```bash
git fetch origin main
git merge --no-commit --no-ff origin/main
```

Expected: conflicts in obligation controller/templates/tests and a duplicate Flyway V11.

- [ ] **Step 2: Resolve unrelated files in favor of `main`**

Keep expense UX, time-zone support, draft conflict APIs, guided-form queue semantics, and all migrations from `main` unchanged.

- [ ] **Step 3: Resolve obligation files by combining both contracts**

The final controller must expose the redesigned enums and validators while retaining the single-draft decision flow, discard endpoint, `fresh=true`, authenticated draft ownership checks, and current recovery metadata.

- [ ] **Step 4: Rename the finance migration and remove the obsolete path**

```bash
git mv src/main/resources/db/migration/V11__redesign_financial_obligations.sql \
  src/main/resources/db/migration/V13__redesign_financial_obligations.sql
```

Append before financial table recreation:

```sql
DELETE FROM form_draft WHERE form_type = 'OBLIGATION';
```

- [ ] **Step 5: Verify the conflict set is empty**

```bash
git diff --check
git status --short | grep '^UU\|^AA\|^DD' && exit 1 || true
```

Expected: no unresolved paths and no whitespace errors.

- [ ] **Step 6: Commit the merge**

```bash
git add -A
git commit -m "merge: integrate current main into obligation redesign"
```

### Task 2: Preserve the Single-Draft Obligation Journey with Purchase Plans

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/web/FinancialObligationController.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/application/ObligationFormSubmissionService.java`
- Modify: `src/main/resources/templates/obligations/form.html`
- Modify: `src/main/resources/templates/obligations/draft-decision.html`
- Test: `src/test/java/dev/harrison/rendacomcarro/finance/ObligationWebTest.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/finance/ObligationFormSubmissionServiceTest.java`

**Interfaces:**
- Produces: `GET /obligations/new?acquisitionPlanId=<uuid>` that uses the single active draft lifecycle and stores the plan ID in schema-2 payload.
- Produces: successful plan-linked submission redirects to `/acquisition-plans/{id}` and completes the exact active draft.

- [ ] **Step 1: Add failing MockMvc tests**

Cover active-draft decision, `fresh=true`, invalid/non-owned `draftKey`, starting from a plan, preserving `acquisitionPlanId`, and plan redirect after save.

- [ ] **Step 2: Run focused tests and verify RED**

```bash
./mvnw -Dtest=ObligationWebTest,ObligationFormSubmissionServiceTest test
```

Expected: failures around decision flow or lost `acquisitionPlanId`.

- [ ] **Step 3: Implement a single `populateForm` path**

The helper must always add vehicles, `draftRecoveryMode`, optional acquisition-plan summary, redesigned enum collections, and the schema-2 form model.

- [ ] **Step 4: Preserve the exact draft completion contract**

```java
drafts.complete(username, FormDraftType.OBLIGATION, form.draftContextKey());
```

Do not call `completeAllOfType`.

- [ ] **Step 5: Run focused tests and verify GREEN**

```bash
./mvnw -Dtest=ObligationWebTest,ObligationFormSubmissionServiceTest test
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/finance \
  src/main/resources/templates/obligations \
  src/test/java/dev/harrison/rendacomcarro/finance
git commit -m "fix: integrate obligation redesign with single draft flow"
```

### Task 3: Separate Canonical and Localized Financial Values

**Files:**
- Modify: `src/main/resources/static/js/financial-input-formatters.js`
- Modify: `src/main/resources/static/js/financial-inputs.js`
- Modify: `src/main/resources/static/js/obligation-form.js`
- Test: `src/test/js/financial-input-formatters.test.mjs`
- Test: `src/test/js/financial-inputs.test.mjs`

**Interfaces:**
- Produces: `parseLocalizedFinancialDecimal(value)` for user text.
- Produces: `parseCanonicalFinancialDecimal(value)` for server/draft values.
- Produces: `formatCanonicalMoney(value)` and `formatCanonicalPercentage(value)`.

- [ ] **Step 1: Write failing tests for canonical ambiguity**

```javascript
test('canonical percentage 0.005 remains five thousandths', () => {
  assert.equal(parseCanonicalFinancialDecimal('0.005'), 0.005);
  assert.equal(formatCanonicalPercentage('0.005'), '0,005');
});

test('canonical money is restored with two decimals', () => {
  assert.equal(formatCanonicalMoney('35000.00'), '35.000,00');
});
```

- [ ] **Step 2: Run focused JavaScript tests and verify RED**

```bash
node --test src/test/js/financial-input-formatters.test.mjs src/test/js/financial-inputs.test.mjs
```

- [ ] **Step 3: Implement explicit parsers and restoration metadata**

Draft restoration must mark restored values as canonical before reinitializing financial inputs. Typed and pasted values continue using the localized parser.

- [ ] **Step 4: Run focused tests and verify GREEN**

```bash
node --test src/test/js/financial-input-formatters.test.mjs src/test/js/financial-inputs.test.mjs
```

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/js/financial-* \
  src/main/resources/static/js/obligation-form.js src/test/js/financial-*.test.mjs
git commit -m "fix: distinguish canonical and localized financial values"
```

### Task 4: Normalize Payment References and Surface Duplicates

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/web/PaymentForm.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/web/FinancialObligationController.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/application/FinancialObligationService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/domain/ObligationPayment.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/finance/FinancialObligationFlowTest.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/finance/ObligationWebTest.java`

**Interfaces:**
- Produces: normalized reference `null` or trimmed text of at most 120 characters.
- Produces: duplicate reference as a field error on `externalReference`.

- [ ] **Step 1: Add failing tests**

Create two payments with blank references and assert both persist. Submit a duplicate nonblank reference and assert the payment form is re-rendered with a field error.

- [ ] **Step 2: Run focused tests and verify RED**

```bash
./mvnw -Dtest=FinancialObligationFlowTest,ObligationWebTest test
```

- [ ] **Step 3: Implement normalization**

```java
private static String normalizeReference(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
}
```

Add `@Size(max = 120)` to the form and normalize before duplicate checking and persistence.

- [ ] **Step 4: Map duplicate errors to the form field**

Catch the domain validation exception in the payment controller and call `result.rejectValue("externalReference", "duplicate", message)`.

- [ ] **Step 5: Run focused tests and verify GREEN**

```bash
./mvnw -Dtest=FinancialObligationFlowTest,ObligationWebTest test
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/finance \
  src/test/java/dev/harrison/rendacomcarro/finance
git commit -m "fix: normalize obligation payment references"
```

### Task 5: Produce Exact Positive Interest-Free Installments

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/application/InstallmentScheduleCalculator.java`
- Modify: `src/main/resources/static/js/obligation-calculator.js`
- Test: `src/test/java/dev/harrison/rendacomcarro/finance/InstallmentScheduleCalculatorTest.java`
- Test: `src/test/js/obligation-calculator.test.mjs`

**Interfaces:**
- Produces: deterministic cent allocation where all entries are positive and totals equal principal.

- [ ] **Step 1: Add failing R$ 1.00 / 18 tests**

Assert 18 entries, no zero total, and exact sum of `1.00` in Java and JavaScript preview.

- [ ] **Step 2: Run focused tests and verify RED**

```bash
./mvnw -Dtest=InstallmentScheduleCalculatorTest test
node --test src/test/js/obligation-calculator.test.mjs
```

- [ ] **Step 3: Implement integer-cent distribution**

For interest-free schedules, convert principal to cents, compute quotient and remainder, and assign one extra cent to the first `remainder` installments. Reject a term greater than principal cents because it would require zero-value installments.

- [ ] **Step 4: Keep browser preview consistent**

Use the same quotient/remainder algorithm for total and displayed installment range; do not simulate a zero final payment.

- [ ] **Step 5: Run focused tests and verify GREEN**

```bash
./mvnw -Dtest=InstallmentScheduleCalculatorTest test
node --test src/test/js/obligation-calculator.test.mjs
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/finance/application/InstallmentScheduleCalculator.java \
  src/main/resources/static/js/obligation-calculator.js \
  src/test/java/dev/harrison/rendacomcarro/finance/InstallmentScheduleCalculatorTest.java \
  src/test/js/obligation-calculator.test.mjs
git commit -m "fix: distribute interest-free installments exactly"
```

### Task 6: Remove Misleading Installment Status and Model Partial Plan Totals

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/application/AcquisitionPlanService.java`
- Modify: `src/main/resources/templates/acquisition-plans/detail.html`
- Modify: `src/main/resources/templates/obligations/detail.html`
- Test: `src/test/java/dev/harrison/rendacomcarro/finance/AcquisitionPlanFlowTest.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/finance/AcquisitionPlanWebTest.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/finance/ObligationWebTest.java`

**Interfaces:**
- Produces: `Summary.totalsComplete()` plus known repayment and known financing-cost subtotals.
- Produces: UI copy `Parcial — existem obrigações sem total calculado` when incomplete.

- [ ] **Step 1: Add failing complete/partial summary tests**

Create one fixed obligation and one `RATE_UNKNOWN` flexible obligation. Assert the summary is incomplete and does not claim the unknown obligation has zero financing cost.

- [ ] **Step 2: Add failing web contract for cronograma without status**

Assert the schedule table no longer renders a `Status` header or per-installment status.

- [ ] **Step 3: Run focused tests and verify RED**

```bash
./mvnw -Dtest=AcquisitionPlanFlowTest,AcquisitionPlanWebTest,ObligationWebTest test
```

- [ ] **Step 4: Implement explicit completeness**

Calculate known subtotals only from obligations with `plannedTotalAmount != null`, expose a boolean completeness flag, and render incomplete labels instead of exact totals.

- [ ] **Step 5: Remove status from the schedule table**

Keep sequence, due date, principal, interest, and total columns only.

- [ ] **Step 6: Run focused tests and verify GREEN**

```bash
./mvnw -Dtest=AcquisitionPlanFlowTest,AcquisitionPlanWebTest,ObligationWebTest test
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/finance/application/AcquisitionPlanService.java \
  src/main/resources/templates/acquisition-plans/detail.html \
  src/main/resources/templates/obligations/detail.html \
  src/test/java/dev/harrison/rendacomcarro/finance
git commit -m "fix: represent partial financing totals honestly"
```

### Task 7: Full Verification and PR Metadata

**Files:**
- Modify: PR #15 description only after tests pass.

**Interfaces:**
- Produces: conflict-free draft PR, final CI evidence, no temporary workflow or payload files.

- [ ] **Step 1: Run all local JavaScript tests**

```bash
npm run test:js
```

Expected: PASS.

- [ ] **Step 2: Run the complete Maven suite and package**

```bash
./mvnw test
./mvnw -DskipTests package
```

Expected: PASS.

- [ ] **Step 3: Run static repository checks**

```bash
git diff --check
git status --short
find src/main/resources/db/migration -maxdepth 1 -type f -printf '%f\n' | sort
```

Expected: clean tree after commits, migrations V11/V12/V13 unique.

- [ ] **Step 4: Push the merge and correction commits to the existing PR branch**

Do not force-push and do not merge the PR.

- [ ] **Step 5: Wait for and inspect complete GitHub Actions CI**

Require JavaScript, Maven/Testcontainers, packaging, credential scan, Compose validation, ARM64 image, production health, backup, and restore jobs to succeed.

- [ ] **Step 6: Remove any temporary workbench workflow and verify final CI again**

The final head must contain only permanent project files.

- [ ] **Step 7: Update the PR body with the final head and verification evidence**

Keep the PR in draft and explicitly state that final merge remains manual.
