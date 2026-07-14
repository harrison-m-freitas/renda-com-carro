# Vehicle Mobile Guided Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a two-step guided flow for vehicle creation and editing below `768px`, while preserving the complete single-page form on desktop.

**Architecture:** The Thymeleaf template declares a vehicle-specific `data-vehicle-*` contract. `vehicle-form.js` remains the page controller for responsive navigation, validation, disclosure, contextual help, dirty-state comparison, cancellation, and submission. `vehicle-form-inputs.js` owns pure plate formatting and cursor preservation. The vehicle form remains outside `data-guided-form` and the draft subsystem.

**Tech Stack:** Java 21, Spring Boot, Thymeleaf, JavaScript ES Modules, Bootstrap/Tabler, Node test runner, Maven, MockMvc, Testcontainers/PostgreSQL, GitHub Actions.

## Execution status

- Tasks 1 through 7 are implemented on `feat/vehicle-mobile-guided-flow`.
- The local JavaScript suite passes with 84 tests and zero failures.
- The exact-head GitHub Actions run is pending infrastructure recovery: recent jobs terminate before checkout and expose no steps or logs.
- No merge or auto-merge has been performed.

## Global constraints

- Activate guided navigation only for `matchMedia('(max-width: 767.98px)')`.
- Use exactly two steps: `identification` and `operation`.
- Keep every section visible at `768px` and wider.
- Validate identification before advancing.
- Open the first step containing a server or browser error.
- Collapse acquisition only on clean mobile forms; open it for an existing value, an error, or an explicit user action.
- Preserve the current year rule (`1980`–`2100`) and all backend rules.
- Preserve no-JavaScript submission and localized inputs.
- Do not add drafts, autosave, migrations, external APIs, or `data-guided-form`.
- Do not merge or enable auto-merge.
- Use TDD and commit after each independently verified task.

---

### Task 1: Declare the HTML contract and field order

**Files:**
- Modify: `src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleWebTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/web/GuidedFormsWebContractTest.java`
- Modify: `src/main/resources/templates/vehicles/form.html`

**Produces:** `data-vehicle-flow`, progress elements, two `data-vehicle-step` regions, acquisition disclosure, previous/next controls, and the existing final submit.

- [x] Add failing MockMvc assertions for the new contract, essential-field order, button types, and absence of draft attributes.
- [x] Move name after plate and rename it `Nome para identificação (opcional)`.
- [x] Add progress markup with copy, name, bar, and ARIA values.
- [x] Mark `identification` and `operation` steps and focusable headings.
- [x] Add a hidden identification error alert.
- [x] Add an accessible acquisition disclosure and panel.
- [x] Add cancel, previous, next, and final-submit contracts while preserving no-JavaScript submission.
- [x] Set make autocomplete to `off` and add a plate HTML pattern/title for `ABC-1234` and `ABC1D23`.

### Task 2: Preserve plate cursor and selection

**Files:**
- Modify: `src/test/js/vehicle-form-inputs.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form-inputs.js`
- Modify: `src/test/js/vehicle-form.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form.js`

**Produces:** `formatVehiclePlateEdit(raw, selectionStart, selectionEnd) -> { value, selectionStart, selectionEnd }`, retaining `formatVehiclePlate(raw)`.

- [x] Cover caret positions around the legacy hyphen, non-collapsed selections, Mercosul, invalid characters, truncation, and idempotence.
- [x] Implement useful-character offset mapping before and after formatting.
- [x] Replace unconditional caret-to-end behavior with `formatVehiclePlateEdit`.

### Task 3: Implement responsive two-step navigation

**Files:**
- Modify: `src/test/js/vehicle-form.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form.js`

- [x] Add a focused fake DOM harness with two steps, headings, progress, controls, actions, and a changeable fake media query.
- [x] Implement clean mobile start, desktop visibility, partial validation, focus on first error, next, back, and resize preservation.
- [x] Synchronize `hidden`, progress copy/name/width/current value, and action visibility.

### Task 4: Route server errors and manage optional acquisition

**Files:**
- Modify: `src/test/js/vehicle-form.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form.js`
- Modify: `src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleWebTest.java`

- [x] Route the first server error to its mobile step.
- [x] Expand acquisition for an existing value, an error, or an explicit user action.
- [x] Keep acquisition visible on desktop.
- [x] Update contextual name help without changing the nickname.
- [x] Add a MockMvc regression for invalid purchase price rendering.

### Task 5: Detect real changes and confirm cancellation

**Files:**
- Modify: `src/test/js/vehicle-form.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form.js`

- [x] Serialize named enabled controls after initialization.
- [x] Replace the one-way dirty boolean with comparison against the initial state.
- [x] Confirm discard only when real changes exist.
- [x] Keep submission and confirmed-discard unload exemptions separate.

### Task 6: Complete final validation and loading state

**Files:**
- Modify: `src/test/js/vehicle-form.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form.js`
- Modify: `src/main/resources/templates/vehicles/form.html`

- [x] Route final and native browser validation to the first visible invalid step.
- [x] Expand acquisition for invalid purchase price.
- [x] Add stable submit spinner/copy children and restore them on `pageshow`.
- [x] Preserve final localized-input formatting before submission.

### Task 7: Style mobile flow and preserve desktop

**Files:**
- Modify: `src/main/resources/static/css/app.css`
- Modify: `src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleWebTest.java`

- [x] Add compact progress, disclosure, focus, validation, safe-area actions, and responsive hidden states.
- [x] Force all steps and acquisition content visible at desktop widths.
- [x] Preserve existing grids and the narrow year/plate breakpoint.
- [x] Add template and stylesheet contract tests.

### Task 8: Full verification and PR readiness

- [x] Run `npm run test:js` locally: 84 tests, zero failures.
- [x] Verify the focused implementation on an intermediate CI head: JavaScript, Java, package, and credential checks passed.
- [ ] Obtain a successful full GitHub Actions run for the exact final head.
- [ ] Record the successful final workflow run in PR `#11`.
- [x] Leave PR open and unmerged for Harrison's manual approval.

## Self-review

- Every approved requirement maps to a task.
- Function and attribute names remain consistent across tasks.
- No entity, service, migration, draft, or external integration is in scope.
- Final identifiers come only from verified command/tool output.
