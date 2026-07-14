# Vehicle Mobile Guided Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a two-step guided flow for vehicle creation and editing below `768px`, while preserving the complete single-page form on desktop.

**Architecture:** The Thymeleaf template declares a vehicle-specific `data-vehicle-*` contract. `vehicle-form.js` remains the page controller for responsive navigation, validation, disclosure, contextual help, dirty-state comparison, cancellation, and submission. `vehicle-form-inputs.js` owns pure plate formatting and cursor preservation. The vehicle form remains outside `data-guided-form` and the draft subsystem.

**Tech Stack:** Java 21, Spring Boot, Thymeleaf, JavaScript ES Modules, Bootstrap/Tabler, Node test runner, Maven, MockMvc, Testcontainers/PostgreSQL, GitHub Actions.

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

- [ ] Add failing MockMvc assertions for the new contract, essential-field order, button types, and absence of draft attributes.
- [ ] Run `./mvnw -q -Dtest=VehicleWebTest,GuidedFormsWebContractTest test` and confirm RED.
- [ ] Move name after plate and rename it `Nome para identificação (opcional)`.
- [ ] Add progress markup with copy, name, bar, and ARIA values.
- [ ] Mark `identification` and `operation` steps and focusable headings.
- [ ] Add a hidden identification error alert.
- [ ] Add an accessible acquisition disclosure and panel.
- [ ] Add cancel, previous, next, and final-submit contracts while preserving no-JavaScript submission.
- [ ] Set make autocomplete to `off` and add a plate HTML pattern/title for `ABC-1234` and `ABC1D23`.
- [ ] Re-run focused Java tests and confirm GREEN.
- [ ] Commit `feat: declare vehicle mobile flow markup`.

### Task 2: Preserve plate cursor and selection

**Files:**
- Modify: `src/test/js/vehicle-form-inputs.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form-inputs.js`
- Modify: `src/test/js/vehicle-form.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form.js`

**Produces:** `formatVehiclePlateEdit(raw, selectionStart, selectionEnd) -> { value, selectionStart, selectionEnd }`, retaining `formatVehiclePlate(raw)`.

- [ ] Add failing tests for caret positions around the legacy hyphen, non-collapsed selections, Mercosul, invalid characters, truncation, and idempotence.
- [ ] Run `node --test src/test/js/vehicle-form-inputs.test.mjs` and confirm RED.
- [ ] Implement useful-character offset mapping before and after formatting.
- [ ] Confirm formatter tests GREEN.
- [ ] Add a failing DOM wiring test proving raw caret `4` becomes rendered caret `5` for `abc1234`.
- [ ] Replace unconditional caret-to-end behavior with `formatVehiclePlateEdit`.
- [ ] Run both vehicle JavaScript test files and confirm GREEN.
- [ ] Commit `fix: preserve vehicle plate edit selection`.

### Task 3: Implement responsive two-step navigation

**Files:**
- Modify: `src/test/js/vehicle-form.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form.js`

**Produces:** `MOBILE_VEHICLE_QUERY`, partial step validation, `getCurrentStep()`, and `showStep(name, options)`.

- [ ] Build a focused fake DOM harness with two steps, headings, progress, controls, actions, and a changeable fake media query.
- [ ] Add failing tests for clean mobile start, desktop visibility, partial validation, focus on first error, next, back, and resize preservation.
- [ ] Run the focused test and confirm RED.
- [ ] Implement step order/labels, `syncFlow`, `showStep`, and partial validation over enabled form controls.
- [ ] Synchronize `hidden`, progress copy/name/width/current value, and action visibility.
- [ ] Focus headings only after explicit next/back actions.
- [ ] Run focused tests and confirm GREEN.
- [ ] Commit `feat: add vehicle mobile step navigation`.

### Task 4: Route server errors and manage optional acquisition

**Files:**
- Modify: `src/test/js/vehicle-form.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form.js`
- Modify: `src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleWebTest.java`

- [ ] Add failing tests for first server-error routing, price-error expansion, clean mobile collapse, desktop visibility, preserved expansion, and contextual name help.
- [ ] Run focused JavaScript tests and confirm RED.
- [ ] Determine the first `.is-invalid` control in DOM order after initial formatting.
- [ ] Implement `setAcquisitionExpanded`, synchronizing `hidden`, `aria-expanded`, and icon state while forcing visibility on desktop.
- [ ] Update name help from trimmed make/model without changing the nickname or dispatching synthetic input.
- [ ] Focus the first server error only on an error-returned mobile page.
- [ ] Add a MockMvc regression for a negative purchase price returned inside operation with `.is-invalid`.
- [ ] Run focused JavaScript and Java tests and confirm GREEN.
- [ ] Commit `feat: guide vehicle errors and optional details`.

### Task 5: Detect real changes and confirm cancellation

**Files:**
- Modify: `src/test/js/vehicle-form.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form.js`

**Produces:** `serializeVehicleForm(form) -> string`, real dirty comparison, and explicit discard confirmation.

- [ ] Add failing tests for change-then-restore, UI-only changes, unchanged cancel, refused discard, confirmed discard, and `beforeunload` exemptions.
- [ ] Run focused tests and confirm RED.
- [ ] Serialize named enabled controls after initial localization and plate formatting, excluding button/submit/reset controls.
- [ ] Replace the one-way dirty boolean with comparison against the initial serialized state.
- [ ] Apply `Descartar as alterações deste veículo?` to every `data-vehicle-cancel` link only when real changes exist.
- [ ] Keep submission and confirmed-discard exemptions separate and restore state correctly on `pageshow`.
- [ ] Run focused tests and confirm GREEN.
- [ ] Commit `fix: confirm only real vehicle form changes`.

### Task 6: Complete final validation and loading state

**Files:**
- Modify: `src/test/js/vehicle-form.test.mjs`
- Modify: `src/main/resources/static/js/vehicle-form.js`
- Modify: `src/main/resources/templates/vehicles/form.html`

- [ ] Add failing tests for final invalid routing, price expansion, loading copy/spinner, and `pageshow` restoration.
- [ ] Run focused tests and confirm RED.
- [ ] Add stable `data-vehicle-submit-spinner` and `data-vehicle-submit-copy` children.
- [ ] Refactor submit state to update children without replacing button DOM.
- [ ] Format localized inputs in final mode before collecting invalid controls in DOM order.
- [ ] Route mobile to the first invalid step, expand acquisition for price, synchronize ARIA/error groups, and focus the first invalid control.
- [ ] On valid submit set submission state and loading UI.
- [ ] Run focused/full JavaScript tests and focused Java tests.
- [ ] Commit `feat: complete vehicle flow validation states`.

### Task 7: Style mobile flow and preserve desktop

**Files:**
- Modify: `src/main/resources/static/css/app.css`
- Modify: `src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleWebTest.java`

- [ ] Assert stable template classes for progress, disclosure, panel, and actions.
- [ ] Add compact progress, 44px disclosure, visible focus, and validation spacing.
- [ ] Below `768px`, hide only enhanced inactive steps/panels, reserve footer space, keep safe-area actions, and ensure hidden actions are not displayed.
- [ ] At `768px` and wider, hide progress/previous/next/disclosure and force both steps/panel visible.
- [ ] Preserve current grids, maximum width, and narrow year/plate breakpoint.
- [ ] Run `npm run test:js` and focused Java tests.
- [ ] Manually inspect `320`, `375`, `768`, and `1280` widths, keyboard focus, horizontal overflow, footer overlap, and reduced motion.
- [ ] Commit `style: refine vehicle guided mobile layout`.

### Task 8: Full verification and PR readiness

- [ ] Run `npm run test:js` with zero failures.
- [ ] Run `./mvnw test` with `BUILD SUCCESS`.
- [ ] Run `./mvnw -DskipTests package` with `BUILD SUCCESS`.
- [ ] Read `.github/workflows/ci.yml` at the final head and execute every locally reproducible validation in declared order.
- [ ] Run `git status --short`, `git diff main...HEAD --stat`, `git diff --check`, and `git log --oneline main..HEAD`.
- [ ] Verify no domain, migration, draft, or unrelated files changed.
- [ ] Push `feat/vehicle-mobile-guided-flow` and verify GitHub Actions on the exact pushed SHA.
- [ ] Update PR `#11` with implemented behavior, exact head SHA, local command results, successful workflow run ID, and confirmation that no merge or auto-merge occurred.
- [ ] Leave PR open for Harrison's manual approval.

## Self-review

- Every approved requirement maps to a task.
- Function and attribute names remain consistent across tasks.
- No entity, service, migration, draft, or external integration is in scope.
- Final identifiers come only from verified command/tool output.
