# Single Active Vehicle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce one active vehicle, remove the separate primary flag, and bind each monthly goal to one immutable vehicle.

**Architecture:** `VehicleStatus.ACTIVE` becomes the only operational-vehicle source of truth, protected by a transactional swap service and a PostgreSQL partial unique index. `MonthlyGoal` changes from a many-to-many set to one `@ManyToOne` vehicle; controllers resolve the vehicle server-side and the browser only displays it.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, Thymeleaf, JavaScript ES Modules, PostgreSQL, Flyway, JUnit 5, MockMvc, Testcontainers, Node test runner.

## Global Constraints

- Never merge or enable auto-merge; leave PR #14 for manual approval.
- Preserve all historical expenses, obligations, shifts and goals on their original vehicle.
- Creating or activating a vehicle automatically inactivates the current active vehicle.
- Archived vehicles cannot be reactivated.
- Active vehicles cannot be archived directly.
- Existing `V13` and `V14` migrations must not be edited; use `V15`.
- New goals use the active vehicle; editing a goal never changes its vehicle.
- The suggestion endpoint must not trust a client-supplied vehicle ID.

---

### Task 1: Vehicle lifecycle and database invariant

**Files:**
- Create: `src/main/resources/db/migration/V15__enforce_single_active_vehicle_and_goal_vehicle.sql`
- Modify: `src/main/java/dev/harrison/rendacomcarro/vehicle/domain/VehicleStatus.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/vehicle/domain/Vehicle.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/vehicle/infrastructure/VehicleRepository.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/vehicle/application/VehicleService.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleServiceTest.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/vehicle/SingleActiveVehicleConstraintTest.java`

**Interfaces:**
- Produces: `Vehicle.activate()`, `Vehicle.deactivate()`, `Vehicle.archive()`, `VehicleService.activate(UUID)`, `VehicleService.findActiveVehicle()`, `VehicleService.getActiveVehicle()`.
- Consumes: `VehicleRepository.findOperationalForUpdate()` returning active and inactive vehicles under `PESSIMISTIC_WRITE`.

- [ ] **Step 1: Write lifecycle tests** asserting second creation inactivates the first, reactivation swaps states, archived reactivation fails, and active archive fails.
- [ ] **Step 2: Run `./mvnw -Dtest=VehicleServiceTest test` in CI and verify failures reference missing `INACTIVE`/new methods.**
- [ ] **Step 3: Add `INACTIVE`, remove `primaryVehicle`, implement transitions and transactional locking.**
- [ ] **Step 4: Add the Flyway migration and a PostgreSQL integration test that direct insertion of a second `ACTIVE` row violates the unique index.**
- [ ] **Step 5: Run vehicle tests and commit `feat: enforce single active vehicle lifecycle`.**

### Task 2: Singular monthly-goal vehicle persistence

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/domain/MonthlyGoal.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/infrastructure/MonthlyGoalRepository.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/application/GoalService.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/goal/MonthlyGoalVehiclePersistenceTest.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/goal/GoalServiceTest.java`

**Interfaces:**
- Produces: `MonthlyGoal.assignVehicle(Vehicle)` used only during creation and `MonthlyGoal.getVehicle()`.
- Consumes: `VehicleRepository.findByStatus(VehicleStatus.ACTIVE)`.

- [ ] **Step 1: Rewrite persistence tests to assert one vehicle and edit preservation after active-vehicle swaps.**
- [ ] **Step 2: Verify tests fail because `MonthlyGoal` still exposes `Set<Vehicle>`.**
- [ ] **Step 3: Replace `@ManyToMany` with non-null `@ManyToOne`; create captures active vehicle; update does not reassign it.**
- [ ] **Step 4: Run goal service and persistence tests.**
- [ ] **Step 5: Commit `refactor: bind monthly goals to one vehicle`.**

### Task 3: Server-owned goal submission and suggestion resolution

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/web/GoalForm.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/application/GoalFormSubmissionService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/web/GoalController.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/web/GoalSuggestionController.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/application/OperationalGoalSuggestion.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/web/OperationalGoalSuggestionResponse.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/application/OperationalGoalSuggestionService.java`
- Modify: expense/obligation/odometer suggestion repositories to accept `UUID vehicleId`.
- Test: `src/test/java/dev/harrison/rendacomcarro/goal/GoalFormSubmissionServiceTest.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/goal/GoalSuggestionWebTest.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/goal/OperationalGoalSuggestionServiceTest.java`
- Test: `src/test/java/dev/harrison/rendacomcarro/goal/OperationalSuggestionRepositoryTest.java`

**Interfaces:**
- Produces endpoint `GET /goals/operational-suggestion?month=YYYY-MM[&goalId=UUID]`.
- New-form resolution uses `VehicleService.getActiveVehicle()`; edit resolution uses `GoalService.get(goalId).getVehicle()`.

- [ ] **Step 1: Write tests rejecting `vehicleIds`, using active vehicle for new goals and stored vehicle for edits.**
- [ ] **Step 2: Verify endpoint/service tests fail on the old set-based contract.**
- [ ] **Step 3: Remove `vehicleIds` from `GoalForm` and submission; convert suggestion contracts and repository queries to one UUID.**
- [ ] **Step 4: Run focused Java tests.**
- [ ] **Step 5: Commit `feat: resolve goal vehicle on the server`.**

### Task 4: Goal form, drafts and JavaScript cleanup

**Files:**
- Modify: `src/main/resources/templates/goals/form.html`
- Modify: `src/main/resources/templates/goals/detail.html`
- Modify: `src/main/resources/static/js/goal-form.js`
- Modify: `src/main/resources/static/js/goal-financial-planner.js`
- Delete: `src/main/resources/static/js/goal-vehicle-picker.js`
- Delete: `src/test/js/goal-vehicle-picker.test.mjs`
- Modify: `src/main/java/dev/harrison/rendacomcarro/draft/application/definition/MonthlyGoalDraftDefinition.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/draft/FormDraftDefinitionTest.java`
- Modify: goal JavaScript and web tests.

**Interfaces:**
- Form root exposes optional `data-goal-id` only; no vehicle input is submitted.
- `buildOperationalSuggestionUrl(month, goalId)` produces the server-owned endpoint URL.

- [ ] **Step 1: Write JS/template tests requiring a read-only vehicle card and absence of `vehicleIds`, picker and chips.**
- [ ] **Step 2: Run `npm run test:js` and verify the new assertions fail.**
- [ ] **Step 3: Remove picker markup/module, render one vehicle context, remove draft field, and update suggestion URL construction.**
- [ ] **Step 4: Run all JavaScript tests and focused MockMvc/draft tests.**
- [ ] **Step 5: Commit `refactor: show one read-only goal vehicle`.**

### Task 5: Vehicle UI and dependent active-vehicle callers

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/vehicle/web/VehicleController.java`
- Modify: `src/main/resources/templates/vehicles/list.html`
- Modify: `src/main/resources/templates/vehicles/detail.html`
- Modify: `src/main/resources/templates/expenses/form.html`
- Modify callers of `findPrimaryVehicle()` / `getPrimaryVehicle()` in expense, odometer and operation controllers and tests.
- Test: `src/test/java/dev/harrison/rendacomcarro/vehicle/VehicleWebTest.java`

**Interfaces:**
- `POST /vehicles/{id}/activate` activates an inactive vehicle.
- `POST /vehicles/{id}/archive` only archives inactive vehicles.

- [ ] **Step 1: Write web tests for status labels, activation, automatic inactivation and blocked active archive.**
- [ ] **Step 2: Verify failures against primary-vehicle copy/actions.**
- [ ] **Step 3: Rename controller/service calls, update templates and flash messages, and remove “Principal” copy.**
- [ ] **Step 4: Run vehicle and dependent flow tests.**
- [ ] **Step 5: Commit `feat: expose active inactive vehicle workflow`.**

### Task 6: Full verification and PR finalization

**Files:**
- Modify: `docs/superpowers/specs/2026-07-15-single-active-vehicle-design.md`
- Modify: `docs/superpowers/plans/2026-07-15-single-active-vehicle.md`
- Modify: PR #14 description.

**Interfaces:**
- Produces one reviewable PR head with no merge or auto-merge.

- [ ] **Step 1: Run `npm run test:js` and `git diff --check` locally.**
- [ ] **Step 2: Publish commits to `feat/monthly-goal-planner-phase-1`.**
- [ ] **Step 3: Wait for complete GitHub Actions CI: JavaScript, Java/Testcontainers, package, credentials, ARM64 smoke, healthcheck, backup and restore.**
- [ ] **Step 4: Inspect review threads and compare branch to `main`.**
- [ ] **Step 5: Update PR description and leave it open for manual review.**
