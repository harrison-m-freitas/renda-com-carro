# Obligation Financing Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reconstruir o cadastro de obrigações, os cálculos de financiamento e o agrupamento de várias fontes em um plano de compra acessível.

**Architecture:** O cálculo financeiro fica em um serviço puro e autoritativo, reutilizado pelo serviço de criação. A interface usa módulos JavaScript puros para prévias e um controlador DOM progressivamente aprimorado. Planos de compra agrupam obrigações sem transformar uma obrigação em uma coleção de credores.

**Tech Stack:** Java 21, Spring Boot 3.5, Thymeleaf, JavaScript ES Modules, Bootstrap/Tabler, PostgreSQL, Flyway, Maven, Node test runner.

## Global Constraints

- Trabalhar na branch `feat/obligation-financing-redesign` baseada em `main`.
- Não fazer merge nem habilitar auto-merge.
- Aplicar TDD: teste vermelho antes de cada mudança de comportamento.
- Manter cálculos e validações autoritativos no backend.
- Preservar melhoria progressiva sem dependência obrigatória de JavaScript.
- Usar controles HTML nativos e linguagem em português brasileiro.
- A migração pode descartar apenas dados das tabelas financeiras de obrigações, pois não existem registros válidos.

---

### Task 1: Motor de cálculo financeiro

**Files:**
- Create: `src/main/java/dev/harrison/rendacomcarro/finance/domain/ObligationCalculationMethod.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/finance/domain/InterestRatePeriod.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/application/InstallmentScheduleCalculator.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/finance/InstallmentScheduleCalculatorTest.java`

**Interfaces:**
- Produces: `calculateFromRate`, `calculateFromInstallment`, `calculateInterestFree`, `calculateSinglePayment`, `estimateFlexible` and records `RepaymentSchedule`, `ScheduleEntry`, `FlexibleEstimate`.

- [ ] Write failing tests for interest-free schedules, known monthly/annual rates, inferred rate for R$ 35.000 in 36 × R$ 1.386, incompatible installments, single payment and flexible payoff.
- [ ] Run the Java test in CI and verify the new API fails to compile before production changes.
- [ ] Implement the calculation methods with deterministic rounding and field-specific calculation exceptions.
- [ ] Re-run the focused Java test in CI and verify all calculator cases pass.
- [ ] Commit the calculator behavior.

### Task 2: New obligation domain and destructive finance migration

**Files:**
- Create: `src/main/resources/db/migration/V11__redesign_financial_obligations.sql`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/domain/ObligationMode.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/domain/ObligationType.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/domain/FinancialObligation.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/domain/ObligationInstallment.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/infrastructure/FinancialObligationRepository.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/application/FinancialObligationService.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/finance/FinancialObligationFlowTest.java`

**Interfaces:**
- Consumes: `InstallmentScheduleCalculator.RepaymentSchedule`.
- Produces: persisted calculation method, normalized rates, installment amount, total repayment and financing cost.

- [ ] Write failing flow tests proving known installment values drive the persisted schedule and totals.
- [ ] Verify the tests fail against the old service.
- [ ] Recreate finance tables and implement the new entity fields and repositories.
- [ ] Update creation to select the correct calculator method and persist generated installments.
- [ ] Verify the focused flow tests pass in CI.
- [ ] Commit the obligation domain migration.

### Task 3: Accessible form model, validation and submission

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/web/ObligationForm.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/finance/web/ObligationFormValidator.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/application/ObligationFormSubmissionService.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/web/FinancialObligationController.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/draft/application/definition/ObligationDraftDefinition.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/finance/ObligationFormSubmissionServiceTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/draft/FormDraftDefinitionTest.java`

**Interfaces:**
- Produces: field-specific validation errors and schema-2 obligation drafts.

- [ ] Write failing tests for every conditional mode, due-date ordering, incompatible values and inactive field removal.
- [ ] Verify failures before implementation.
- [ ] Implement the new form properties, validator and normalized command mapping.
- [ ] Upgrade draft schema, allow new fields and migrate legacy payload names and enum values.
- [ ] Verify focused submission and draft tests pass in CI.
- [ ] Commit form and draft behavior.

### Task 4: Natural financial inputs and browser calculator

**Files:**
- Create: `src/main/resources/static/js/financial-input-formatters.js`
- Create: `src/main/resources/static/js/financial-inputs.js`
- Create: `src/main/resources/static/js/obligation-calculator.js`
- Create: `src/main/resources/static/js/obligation-form-state.js`
- Modify: `src/test/js/localized-input-formatters.test.mjs`
- Create: `src/test/js/financial-input-formatters.test.mjs`
- Create: `src/test/js/obligation-calculator.test.mjs`
- Create: `src/test/js/obligation-form-state.test.mjs`

**Interfaces:**
- Produces: natural money/percentage formatting, browser-only preview calculations and active-payload selection.

- [ ] Write failing Node tests for typing, paste, blur formatting, rate inference, totals, dates and payload filtering.
- [ ] Run `npm run test:js` and confirm the new tests fail.
- [ ] Implement the pure modules without DOM dependencies.
- [ ] Run `npm run test:js` and confirm all JavaScript tests pass.
- [ ] Commit the reusable browser behavior.

### Task 5: Shadcn-inspired obligation interface

**Files:**
- Rewrite: `src/main/resources/templates/obligations/form.html`
- Rewrite: `src/main/resources/static/js/obligation-form.js`
- Create: `src/main/resources/static/css/obligation-form.css`
- Modify: `src/test/java/dev/harrison/rendacomcarro/finance/ObligationWebTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/web/GuidedFormsWebContractTest.java`

**Interfaces:**
- Consumes: form model, guided-form events, financial input modules and browser calculator.
- Produces: accessible four-step form, live summary, submit protection and server error summary.

- [ ] Write failing MockMvc contracts for fieldsets, radios, ARIA relationships, error summary, natural masks and script/style modules.
- [ ] Verify the contracts fail on the old template.
- [ ] Implement the new template, CSS and DOM controller.
- [ ] Run JavaScript tests and focused web tests in CI.
- [ ] Commit the obligation interface.

### Task 6: Acquisition plans with multiple funding sources

**Files:**
- Create: `src/main/java/dev/harrison/rendacomcarro/finance/domain/AcquisitionPlan.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/finance/infrastructure/AcquisitionPlanRepository.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/finance/application/AcquisitionPlanService.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/finance/web/AcquisitionPlanForm.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/finance/web/AcquisitionPlanController.java`
- Create: `src/main/resources/templates/acquisition-plans/list.html`
- Create: `src/main/resources/templates/acquisition-plans/form.html`
- Create: `src/main/resources/templates/acquisition-plans/detail.html`
- Create: `src/main/resources/static/js/acquisition-plan-form.js`
- Modify: `src/main/resources/templates/obligations/list.html`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/domain/FinancialObligation.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/application/FinancialObligationService.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/finance/AcquisitionPlanFlowTest.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/finance/AcquisitionPlanWebTest.java`

**Interfaces:**
- Produces: purchase coverage summary and contextual obligation creation.

- [ ] Write failing tests for plan creation, coverage with two obligations, remaining amount and contextual redirects.
- [ ] Verify tests fail before production classes exist.
- [ ] Implement entity, service, controller, templates and obligation association.
- [ ] Add explicit “usar valor restante” behavior without automatic hidden changes.
- [ ] Verify focused flow/web tests pass in CI.
- [ ] Commit acquisition planning.

### Task 7: Detail views, regression suite and final verification

**Files:**
- Modify: `src/main/resources/templates/obligations/detail.html`
- Modify: `src/main/resources/templates/obligations/list.html`
- Modify: `src/main/resources/templates/obligations/payment-form.html`
- Modify: `README.md`
- Modify: finance tests as required by final contracts.

**Interfaces:**
- Consumes: persisted totals, rates, schedule and acquisition context.

- [ ] Write failing view assertions for totals, cost, rates, schedule and plan link.
- [ ] Implement the final detail/list presentation and update documentation.
- [ ] Run `npm run test:js` locally.
- [ ] Run the complete GitHub Actions workflow for the exact head, including Java, MockMvc, Testcontainers, package, Docker/Compose and operational checks.
- [ ] Inspect the complete diff against `main`, verify no unrelated files changed and update the draft PR description.
- [ ] Leave the PR open for manual review without merge or auto-merge.
