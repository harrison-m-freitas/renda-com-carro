# Guided Forms and Synced Drafts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build guided desktop/mobile forms for expenses, mileage closings, monthly goals, and financial obligations, with synchronized seven-day drafts, conflict protection, clearer localized inputs, and progressive enhancement.

**Architecture:** Add a modular `draft` package containing the generic `FormDraft` aggregate, PostgreSQL persistence, per-form payload definitions, application service, cleanup job, and authenticated JSON API. Each business module keeps its own form and final submission rules, while a small transactional submission coordinator creates the real record and removes the matching draft atomically. Shared CSS and ES-module JavaScript provide the desktop sections, mobile wizard, autosave, recovery, local emergency cache, and conflict UI without making JavaScript mandatory for final submission.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring MVC, Spring Data JPA/Hibernate 6, Spring Security, Thymeleaf, Bootstrap 5.3.7, PostgreSQL 17, Flyway, Jackson, vanilla ES modules, Node.js built-in test runner, JUnit 5, MockMvc, Testcontainers.

## Global Constraints

- Preserve Java 21, Spring Boot 3.5.14, PostgreSQL 17, Flyway, Thymeleaf, and Bootstrap 5.3.7.
- Do not modify Flyway migrations `V1` through `V7`; create `V8__create_form_drafts.sql`.
- Guided forms apply only to Expenses, Mileage Closings, Monthly Goals, and Financial Obligations.
- Desktop shows all sections on one page; mobile shows one step at a time.
- Autosave debounce is exactly 1,500 ms and `Continue` waits for a successful server save.
- Every valid draft update renews expiration to seven days after `updatedAt`.
- Maximum serialized draft payload is 65,536 UTF-8 bytes; maximum context key length is 160 characters.
- Drafts never make derived values authoritative; final submissions recalculate all totals, ratios, projections, and mileage.
- Unknown JSON fields are rejected.
- Drafts cannot contain binary attachments.
- Conflict handling never overwrites a newer version silently; force replacement requires an explicit user action.
- No automatic field-by-field merge is added.
- Thymeleaf escaping and assigning restored values through DOM `.value`/`.checked` are mandatory; never insert draft text with `innerHTML`.
- Without JavaScript, all fields remain visible and the four forms can still be submitted normally.
- All controllers and draft API endpoints remain authenticated and CSRF-protected.
- Final record creation and matching draft deletion occur in the same database transaction.
- Use TDD: add a focused failing test, verify the expected failure, implement the minimum behavior, rerun the focused test, then commit.
- Do not merge to `main` during implementation; open a pull request and wait for review and green CI.

---

## File and Responsibility Map

### New draft module

- `src/main/java/dev/harrison/rendacomcarro/draft/domain/FormDraftType.java` — the four supported draft types.
- `src/main/java/dev/harrison/rendacomcarro/draft/domain/FormDraft.java` — owner-scoped aggregate, JSON payload, optimistic version, timestamps, and expiration.
- `src/main/java/dev/harrison/rendacomcarro/draft/infrastructure/FormDraftRepository.java` — owner/type/context persistence queries.
- `src/main/java/dev/harrison/rendacomcarro/draft/application/FormDraftDefinition.java` — per-form payload contract.
- `src/main/java/dev/harrison/rendacomcarro/draft/application/FormDraftDefinitionRegistry.java` — type-to-definition lookup.
- `src/main/java/dev/harrison/rendacomcarro/draft/application/DraftPayloadValidator.java` — shared JSON shape, text, date, number, and UUID validation.
- `src/main/java/dev/harrison/rendacomcarro/shared/domain/FlexibleDecimalParser.java` — one parser shared by JSON drafts and MVC form binding.
- `src/main/java/dev/harrison/rendacomcarro/draft/application/definition/ExpenseDraftDefinition.java` — expense fields, steps, and `current` key.
- `src/main/java/dev/harrison/rendacomcarro/draft/application/definition/MileageClosingDraftDefinition.java` — vehicle/month key and manual correction payload rules.
- `src/main/java/dev/harrison/rendacomcarro/draft/application/definition/MonthlyGoalDraftDefinition.java` — month key and goal payload rules.
- `src/main/java/dev/harrison/rendacomcarro/draft/application/definition/ObligationDraftDefinition.java` — multiple obligation drafts and mode-dependent rules.
- `src/main/java/dev/harrison/rendacomcarro/draft/application/FormDraftService.java` — create, load, save, list, discard, complete, and expire drafts.
- `src/main/java/dev/harrison/rendacomcarro/draft/application/FormDraftConflictException.java` — carries the current server version for HTTP 409.
- `src/main/java/dev/harrison/rendacomcarro/draft/application/FormDraftCleanupJob.java` — daily idempotent cleanup.
- `src/main/java/dev/harrison/rendacomcarro/draft/config/DraftConfiguration.java` — application clock and scheduling.
- `src/main/java/dev/harrison/rendacomcarro/draft/web/FormDraftController.java` — authenticated JSON API.
- `src/main/java/dev/harrison/rendacomcarro/draft/web/FormDraftApiExceptionHandler.java` — JSON 400/404/409 responses.
- `src/main/java/dev/harrison/rendacomcarro/draft/web/SaveFormDraftRequest.java` — save request DTO.
- `src/main/java/dev/harrison/rendacomcarro/draft/web/FormDraftResponse.java` — full recovery/conflict DTO.
- `src/main/java/dev/harrison/rendacomcarro/draft/web/FormDraftSummaryResponse.java` — obligation draft list DTO.
- `src/main/resources/db/migration/V8__create_form_drafts.sql` — table, constraints, and indexes.

### Shared presentation infrastructure

- `src/main/java/dev/harrison/rendacomcarro/shared/web/FlexibleBigDecimalEditor.java` — accepts Brazilian or technical decimal text.
- `src/main/java/dev/harrison/rendacomcarro/shared/web/WebBindingAdvice.java` — registers decimal binding for MVC forms.
- `src/main/resources/templates/fragments/guided-form.html` — reusable save status, recovery modal, conflict modal, and mobile actions.
- `src/main/resources/templates/fragments/head.html` — CSRF metadata.
- `src/main/resources/static/css/app.css` — guided sections, read-only values, mobile wizard, sticky action bar, focus, and 44 px targets.
- `src/main/resources/static/js/form-draft-client.js` — draft HTTP API and emergency local cache.
- `src/main/resources/static/js/guided-form-state.js` — pure state helpers testable in Node.
- `src/main/resources/static/js/guided-form.js` — DOM controller for steps, validation, autosave, recovery, and conflicts.
- `package.json` — dependency-free Node test command.
- `src/test/js/guided-form-state.test.mjs` — pure wizard and draft-key tests.
- `src/test/js/form-draft-client.test.mjs` — request construction and local-cache tests.

### Module integrations

- Expense: `ExpenseForm`, `ExpenseController`, new `ExpenseFormSubmissionService`, `expenses/form.html`, and `expense-form.js`.
- Mileage closing: `MonthlyOdometerClosingForm`, `MonthlyOdometerClosingController`, new `MonthlyClosingFormSubmissionService`, `mileage-closings/form.html`, and `mileage-closing-form.js`.
- Goal: `GoalForm`, `GoalController`, new `GoalFormSubmissionService`, `goals/form.html`, and new `goal-form.js`.
- Obligation: `ObligationForm`, `FinancialObligationController`, new `ObligationFormSubmissionService`, `obligations/form.html`, `obligations/list.html`, and new `obligation-form.js`.

### Tests and documentation

- New draft persistence, definition, service, API, binding, and web contract tests.
- Extend existing expense and mileage tests.
- Add goal and obligation web tests.
- Extend `AcceptanceFlowTest`.
- Update `README.md`, `docs/architecture.md`, and `docs/mvp-acceptance-checklist.md`.
- Update `.github/workflows/ci.yml` to run dependency-free JavaScript tests before Maven tests.


---

## Execution Preparation

At implementation time, create an isolated worktree from the approved design branch so the committed specification and this plan are already present:

```bash
git fetch origin
git worktree add ../renda-com-carro-guided-forms \
  -b feature/guided-forms-synced-drafts \
  origin/design/form-guidance-drafts
cd ../renda-com-carro-guided-forms
./mvnw clean test
```

Expected baseline: `BUILD SUCCESS`. Stop and diagnose any baseline failure before Task 1.

---

### Task 1: Create the FormDraft schema and persistence aggregate

**Files:**
- Create: `src/main/resources/db/migration/V8__create_form_drafts.sql`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/domain/FormDraftType.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/domain/FormDraft.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/infrastructure/FormDraftRepository.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/draft/FormDraftPersistenceTest.java`

**Interfaces:**
- Consumes: `AppUser` from `security.domain` and PostgreSQL/Flyway already configured by the application.
- Produces:
  - `FormDraft.create(AppUser, FormDraftType, String, int, int, String, LocalDateTime, LocalDateTime)`
  - `FormDraft.update(int, int, String, LocalDateTime, LocalDateTime)`
  - `FormDraftRepository.findByOwnerUsernameAndFormTypeAndContextKey(...)`
  - `FormDraftRepository.findAllByOwnerUsernameAndFormTypeAndExpiresAtAfterOrderByUpdatedAtDesc(...)`
  - `FormDraftRepository.deleteByExpiresAtLessThanEqual(...)`

- [ ] **Step 1: Write the failing persistence test**

```java
@SpringBootTest
@TestPropertySource(properties = {
    "APP_ADMIN_USERNAME=draft-owner",
    "APP_ADMIN_PASSWORD=draft-owner-password"
})
class FormDraftPersistenceTest extends PostgresIntegrationTest {
    @Autowired AppUserRepository users;
    @Autowired FormDraftRepository drafts;

    @Test
    void storesJsonAndEnforcesOneDraftPerOwnerTypeAndContext() {
        AppUser owner = users.findByUsername("draft-owner").orElseThrow();
        LocalDateTime now = LocalDateTime.of(2026, 7, 13, 10, 0);

        drafts.saveAndFlush(FormDraft.create(
            owner,
            FormDraftType.EXPENSE,
            "current",
            1,
            1,
            "{\"amount\":\"120,50\"}",
            now,
            now.plusDays(7)
        ));

        FormDraft restored = drafts.findByOwnerUsernameAndFormTypeAndContextKey(
            "draft-owner", FormDraftType.EXPENSE, "current"
        ).orElseThrow();

        assertThat(restored.getPayloadJson()).isEqualTo("{\"amount\":\"120,50\"}");
        assertThat(restored.getVersion()).isZero();

        assertThatThrownBy(() -> drafts.saveAndFlush(FormDraft.create(
            owner,
            FormDraftType.EXPENSE,
            "current",
            1,
            1,
            "{}",
            now,
            now.plusDays(7)
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }
}
```

- [ ] **Step 2: Run the focused test and verify the expected failure**

Run:

```bash
./mvnw -Dtest=FormDraftPersistenceTest test
```

Expected: compilation fails because `FormDraft`, `FormDraftType`, and `FormDraftRepository` do not exist.

- [ ] **Step 3: Add the Flyway migration**

```sql
CREATE TABLE form_draft (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES app_user(id),
    form_type VARCHAR(40) NOT NULL,
    context_key VARCHAR(160) NOT NULL,
    schema_version INTEGER NOT NULL CHECK (schema_version > 0),
    current_step INTEGER NOT NULL CHECK (current_step > 0),
    payload_json JSONB NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_form_draft_owner_type_context
        UNIQUE (owner_id, form_type, context_key)
);

CREATE INDEX idx_form_draft_owner_type_updated
    ON form_draft(owner_id, form_type, updated_at DESC);

CREATE INDEX idx_form_draft_expires_at
    ON form_draft(expires_at);
```

- [ ] **Step 4: Implement the enum, entity, and repository**

```java
public enum FormDraftType {
    EXPENSE,
    MILEAGE_CLOSING,
    MONTHLY_GOAL,
    OBLIGATION
}
```

Use `@JdbcTypeCode(SqlTypes.JSON)` on the `String payloadJson` field, `@Version` on `long version`, and the exact table/column names from the migration.

```java
public static FormDraft create(
    AppUser owner,
    FormDraftType type,
    String contextKey,
    int schemaVersion,
    int currentStep,
    String payloadJson,
    LocalDateTime createdAt,
    LocalDateTime expiresAt
) {
    FormDraft draft = new FormDraft();
    draft.id = UUID.randomUUID();
    draft.owner = Objects.requireNonNull(owner);
    draft.formType = Objects.requireNonNull(type);
    draft.contextKey = contextKey;
    draft.schemaVersion = schemaVersion;
    draft.currentStep = currentStep;
    draft.payloadJson = payloadJson;
    draft.createdAt = createdAt;
    draft.updatedAt = createdAt;
    draft.expiresAt = expiresAt;
    return draft;
}

public void update(
    int schemaVersion,
    int currentStep,
    String payloadJson,
    LocalDateTime updatedAt,
    LocalDateTime expiresAt
) {
    this.schemaVersion = schemaVersion;
    this.currentStep = currentStep;
    this.payloadJson = payloadJson;
    this.updatedAt = updatedAt;
    this.expiresAt = expiresAt;
}
```

Repository signatures:

```java
Optional<FormDraft> findByOwnerUsernameAndFormTypeAndContextKey(
    String username,
    FormDraftType formType,
    String contextKey
);

List<FormDraft> findAllByOwnerUsernameAndFormTypeAndExpiresAtAfterOrderByUpdatedAtDesc(
    String username,
    FormDraftType formType,
    LocalDateTime now
);

long deleteByExpiresAtLessThanEqual(LocalDateTime cutoff);
```

- [ ] **Step 5: Run the focused persistence test**

Run:

```bash
./mvnw -Dtest=FormDraftPersistenceTest test
```

Expected: `BUILD SUCCESS`; JSON round-trips and the unique constraint is enforced.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/db/migration/V8__create_form_drafts.sql \
        src/main/java/dev/harrison/rendacomcarro/draft \
        src/test/java/dev/harrison/rendacomcarro/draft/FormDraftPersistenceTest.java
git commit -m "feat: add synchronized form draft persistence"
```

---

### Task 2: Define and validate the four draft payload contracts

**Files:**
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/application/FormDraftDefinition.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/application/FormDraftDefinitionRegistry.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/application/DraftPayloadValidator.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/shared/domain/FlexibleDecimalParser.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/application/definition/ExpenseDraftDefinition.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/application/definition/MileageClosingDraftDefinition.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/application/definition/MonthlyGoalDraftDefinition.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/application/definition/ObligationDraftDefinition.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/draft/FormDraftDefinitionTest.java`

**Interfaces:**
- Consumes: `FormDraftType`, Jackson `ObjectMapper`, `ObjectNode`, and `DomainValidationException`.
- Produces:

```java
public interface FormDraftDefinition {
    FormDraftType type();
    int schemaVersion();
    int maxStep();
    String normalizeContextKey(String contextKey);
    ObjectNode normalizeAndValidate(ObjectNode payload, int currentStep);
}
```

```java
public FormDraftDefinition require(FormDraftType type)
```

- [ ] **Step 1: Write failing tests for allowed fields, keys, steps, and conditional requirements**

Include these exact cases:

```java
@Test
void expenseRejectsUnknownFieldAndNormalizesCurrentKey() {
    ObjectNode payload = mapper.createObjectNode()
        .put("vehicleId", UUID.randomUUID().toString())
        .put("categoryId", UUID.randomUUID().toString())
        .put("expenseDate", "2026-07-13")
        .put("competenceMonth", "2026-07")
        .put("amount", "120,50")
        .put("classification", "PROFESSIONAL");

    assertThat(expense.normalizeContextKey(" current ")).isEqualTo("current");
    assertThatNoException().isThrownBy(() -> expense.normalizeAndValidate(payload, 2));

    payload.put("calculatedProfessionalAmount", "120.50");
    assertThatThrownBy(() -> expense.normalizeAndValidate(payload, 2))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("Campo de rascunho não permitido");
}

@Test
void manualMileageCorrectionRequiresReason() {
    ObjectNode payload = mapper.createObjectNode()
        .put("manualAdjustment", true)
        .put("initialOdometer", "10000,0")
        .put("finalOdometer", "10100,0")
        .put("professionalKilometers", "80,0");

    assertThatThrownBy(() -> mileage.normalizeAndValidate(payload, 3))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("justificativa");
}

@Test
void obligationStructuredModeRequiresScheduleFieldsAtStepThree() {
    ObjectNode payload = mapper.createObjectNode()
        .put("creditor", "Banco")
        .put("type", "BANK_FINANCING")
        .put("mode", "STRUCTURED")
        .put("principal", "30000,00")
        .put("startDate", "2026-07-13");

    assertThatThrownBy(() -> obligation.normalizeAndValidate(payload, 3))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("primeiro vencimento");
}
```

- [ ] **Step 2: Run the definition test and verify failure**

Run:

```bash
./mvnw -Dtest=FormDraftDefinitionTest test
```

Expected: compilation fails because the definition classes do not exist.

- [ ] **Step 3: Implement the shared validator**

Implement these exact helper responsibilities:

```java
public void rejectUnknownFields(ObjectNode payload, Set<String> allowedFields)
public String requireText(ObjectNode payload, String field, String label)
public String optionalText(ObjectNode payload, String field)
public UUID requireUuid(ObjectNode payload, String field, String label)
public LocalDate requireDate(ObjectNode payload, String field, String label)
public YearMonth requireYearMonth(ObjectNode payload, String field, String label)
public BigDecimal requireDecimal(ObjectNode payload, String field, String label)
public boolean booleanValue(ObjectNode payload, String field)
public ObjectNode sanitizeTextFields(ObjectNode payload, Set<String> textFields)
```

Create the shared parser now so Task 5 does not duplicate number rules:

```java
public final class FlexibleDecimalParser {
    private FlexibleDecimalParser() {}

    public static BigDecimal parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim()
            .replace("\u00A0", "")
            .replace(" ", "");
        if (normalized.contains(",") && normalized.contains(".")) {
            normalized = normalized.replace(".", "").replace(",", ".");
        } else if (normalized.contains(",")) {
            normalized = normalized.replace(",", ".");
        }
        return new BigDecimal(normalized);
    }
}
```

`requireDecimal` must accept both `"1234.56"` and `"1.234,56"` by delegating to `FlexibleDecimalParser.parse`. `sanitizeTextFields` removes NUL and non-whitespace control characters but preserves `\n`, `\r`, and `\t`.

- [ ] **Step 4: Implement the exact field sets and step rules**

Expense fields:

```text
vehicleId, categoryId, expenseDate, competenceMonth, paidDate, amount,
classification, allocationMethod, professionalPercentagePercent,
professionalFixedAmount, adjustmentReason, notes
```

Expense key: only `current`; schema version `1`; maximum step `3`.

Mileage fields:

```text
manualAdjustment, initialOdometer, finalOdometer,
professionalKilometers, adjustmentReason, confirmWarnings
```

Mileage key regex:

```regex
vehicle:[0-9a-fA-F-]{36}:month:\d{4}-\d{2}
```

Schema version `1`; maximum step `3`. When `manualAdjustment` is false, remove `initialOdometer`, `finalOdometer`, `professionalKilometers`, and `adjustmentReason` from the normalized payload. When true, all three values and a non-blank reason are required at step 3.

Goal fields:

```text
month, personalNetGoal, operationalGoal, plannedHours, plannedDates
```

Goal key regex:

```regex
month:\d{4}-\d{2}
```

Schema version `1`; maximum step `3`. `plannedDates` remains a string of ISO dates separated by commas, semicolons, whitespace, or line breaks; validation rejects dates outside the selected month and Sundays.

Obligation fields:

```text
vehicleId, creditor, type, mode, principal, annualRatePercent,
startDate, firstDueDate, termMonths, plannedInstallment,
monthlyTarget, notes
```

Obligation key regex:

```regex
draft:[0-9a-fA-F-]{36}
```

Schema version `1`; maximum step `4`. `STRUCTURED` requires `firstDueDate` and positive `termMonths` at step 3. `FLEXIBLE` requires a positive `monthlyTarget` at step 3. Fields irrelevant to the chosen mode are removed during normalization.

- [ ] **Step 5: Implement and test the registry**

```java
@Component
public class FormDraftDefinitionRegistry {
    private final Map<FormDraftType, FormDraftDefinition> definitions;

    public FormDraftDefinitionRegistry(List<FormDraftDefinition> definitions) {
        this.definitions = definitions.stream().collect(Collectors.toUnmodifiableMap(
            FormDraftDefinition::type,
            Function.identity()
        ));
    }

    public FormDraftDefinition require(FormDraftType type) {
        FormDraftDefinition definition = definitions.get(type);
        if (definition == null) {
            throw new DomainValidationException("Tipo de rascunho não suportado.");
        }
        return definition;
    }
}
```

Run:

```bash
./mvnw -Dtest=FormDraftDefinitionTest test
```

Expected: all contract tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/draft/application \
        src/main/java/dev/harrison/rendacomcarro/shared/domain/FlexibleDecimalParser.java \
        src/test/java/dev/harrison/rendacomcarro/draft/FormDraftDefinitionTest.java
git commit -m "feat: validate draft payloads by form type"
```

---

### Task 3: Implement draft lifecycle, optimistic conflict handling, and expiration

**Files:**
- Create: `src/main/java/dev/harrison/rendacomcarro/security/application/CurrentUserService.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/application/FormDraftConflictException.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/application/FormDraftService.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/application/FormDraftCleanupJob.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/config/DraftConfiguration.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/draft/FormDraftServiceTest.java`

**Interfaces:**
- Consumes: persistence and definitions from Tasks 1–2.
- Produces:

```java
public record SaveDraftCommand(
    FormDraftType formType,
    String contextKey,
    int schemaVersion,
    int currentStep,
    Long expectedVersion,
    ObjectNode payload,
    boolean force
) {}
```

```java
public record DraftView(
    UUID id,
    FormDraftType formType,
    String contextKey,
    int schemaVersion,
    int currentStep,
    ObjectNode payload,
    long version,
    LocalDateTime updatedAt,
    LocalDateTime expiresAt
) {}
```

```java
Optional<DraftView> find(String username, FormDraftType type, String contextKey)
DraftView save(String username, SaveDraftCommand command)
void discard(String username, FormDraftType type, String contextKey, Long expectedVersion)
List<DraftView> listActive(String username, FormDraftType type)
void complete(String username, FormDraftType type, String contextKey)
long deleteExpired()
```

- [ ] **Step 1: Write failing service tests**

Cover:

1. first save creates version `0`;
2. second save with version `0` returns version `1`;
3. stale version returns `FormDraftConflictException` containing the server `DraftView`;
4. `force=true` replaces the current payload;
5. update renews expiration to exactly seven days;
6. expired drafts are not returned;
7. discard enforces ownership and version;
8. `complete` is idempotent;
9. payloads larger than 65,536 UTF-8 bytes are rejected;
10. obligation list returns only active drafts for the authenticated owner.

Use a fixed clock:

```java
@TestConfiguration
static class ClockConfiguration {
    @Bean
    @Primary
    Clock testClock() {
        return Clock.fixed(
            Instant.parse("2026-07-13T13:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
        );
    }
}
```

- [ ] **Step 2: Run the service test and verify failure**

Run:

```bash
./mvnw -Dtest=FormDraftServiceTest test
```

Expected: compilation fails because the service and conflict exception do not exist.

- [ ] **Step 3: Implement current-user lookup and clock configuration**

```java
@Service
public class CurrentUserService {
    private final AppUserRepository users;

    public CurrentUserService(AppUserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public AppUser require(String username) {
        return users.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));
    }
}
```

```java
@Configuration
@EnableScheduling
public class DraftConfiguration {
    @Bean
    Clock applicationClock(@Value("${app.timezone}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
```

- [ ] **Step 4: Implement save and conflict semantics**

Core save flow:

```java
@Transactional
public DraftView save(String username, SaveDraftCommand command) {
    FormDraftDefinition definition = definitions.require(command.formType());
    if (command.schemaVersion() != definition.schemaVersion()) {
        throw new DomainValidationException("Versão de rascunho incompatível.");
    }
    if (command.currentStep() < 1 || command.currentStep() > definition.maxStep()) {
        throw new DomainValidationException("Etapa de rascunho inválida.");
    }

    String contextKey = definition.normalizeContextKey(command.contextKey());
    ObjectNode normalized = definition.normalizeAndValidate(
        command.payload().deepCopy(),
        command.currentStep()
    );
    String json = writeAndCheckSize(normalized);
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDateTime expiresAt = now.plusDays(7);

    Optional<FormDraft> existing = repository
        .findByOwnerUsernameAndFormTypeAndContextKey(
            username, command.formType(), contextKey
        );

    if (existing.isEmpty()) {
        if (command.expectedVersion() != null) {
            throw new DomainConflictException("O rascunho informado não existe mais.");
        }
        FormDraft created = FormDraft.create(
            currentUser.require(username),
            command.formType(),
            contextKey,
            command.schemaVersion(),
            command.currentStep(),
            json,
            now,
            expiresAt
        );
        return toView(repository.saveAndFlush(created));
    }

    FormDraft draft = existing.orElseThrow();
    if (!command.force()
        && (command.expectedVersion() == null
            || draft.getVersion() != command.expectedVersion())) {
        throw new FormDraftConflictException(toView(draft));
    }

    draft.update(
        command.schemaVersion(),
        command.currentStep(),
        json,
        now,
        expiresAt
    );
    return toView(repository.saveAndFlush(draft));
}
```

Catch `ObjectOptimisticLockingFailureException` and re-read the current draft before throwing `FormDraftConflictException`.

- [ ] **Step 5: Implement find, list, discard, complete, size limit, and cleanup**

- `find` returns empty and deletes nothing when `expiresAt <= now`.
- `listActive` uses the repository query with `expiresAt > now`.
- `discard` rejects a stale version unless the expected version is null because the recovery dialog explicitly chose “start again”.
- `complete` deletes by owner/type/context without error when no draft exists.
- `writeAndCheckSize` serializes with `ObjectMapper`, checks UTF-8 bytes, and throws `DomainValidationException("O rascunho excede o limite de 64 KiB.")`.
- `deleteExpired` delegates to `deleteByExpiresAtLessThanEqual(now)`.

Cleanup job:

```java
@Component
public class FormDraftCleanupJob {
    private final FormDraftService drafts;

    public FormDraftCleanupJob(FormDraftService drafts) {
        this.drafts = drafts;
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "${app.timezone}")
    public void removeExpiredDrafts() {
        drafts.deleteExpired();
    }
}
```

- [ ] **Step 6: Run the focused service tests**

Run:

```bash
./mvnw -Dtest=FormDraftServiceTest test
```

Expected: all lifecycle, conflict, ownership, size, and expiration tests pass.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/security/application/CurrentUserService.java \
        src/main/java/dev/harrison/rendacomcarro/draft \
        src/test/java/dev/harrison/rendacomcarro/draft/FormDraftServiceTest.java
git commit -m "feat: manage draft lifecycle and conflicts"
```

---

### Task 4: Expose the authenticated draft JSON API

**Files:**
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/web/SaveFormDraftRequest.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/web/FormDraftResponse.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/web/FormDraftSummaryResponse.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/web/FormDraftController.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/draft/web/FormDraftApiExceptionHandler.java`
- Modify: `src/main/resources/templates/fragments/head.html`
- Create: `src/test/java/dev/harrison/rendacomcarro/draft/FormDraftApiTest.java`

**Interfaces:**
- Consumes: `FormDraftService`.
- Produces:
  - `GET /api/form-drafts/{type}?contextKey=...`
  - `PUT /api/form-drafts/{type}`
  - `DELETE /api/form-drafts/{type}?contextKey=...&version=...`
  - `GET /api/form-drafts/OBLIGATION/list`

- [ ] **Step 1: Write failing MockMvc API tests**

Exact assertions:

```java
mvc.perform(put("/api/form-drafts/EXPENSE")
        .with(csrf())
        .with(user("draft-api-owner").roles("OWNER"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "contextKey":"current",
              "schemaVersion":1,
              "currentStep":1,
              "version":null,
              "force":false,
              "payload":{
                "vehicleId":"%s",
                "categoryId":"%s",
                "expenseDate":"2026-07-13",
                "competenceMonth":"2026-07",
                "amount":"100,00",
                "classification":"PROFESSIONAL"
              }
            }
            """.formatted(vehicleId, categoryId)))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.contextKey").value("current"))
    .andExpect(jsonPath("$.version").value(0));
```

Also verify:

- no authentication returns 3xx login redirect or 401 according to current security behavior;
- missing CSRF returns 403;
- unknown field returns JSON 400;
- missing draft returns JSON 404;
- stale version returns 409 and `$.current.version`;
- obligation list does not expose another owner’s drafts;
- delete removes the draft.

- [ ] **Step 2: Run the API test and verify failure**

Run:

```bash
./mvnw -Dtest=FormDraftApiTest test
```

Expected: 404 because the API controller does not exist.

- [ ] **Step 3: Implement request and response records**

```java
public record SaveFormDraftRequest(
    String contextKey,
    int schemaVersion,
    int currentStep,
    Long version,
    boolean force,
    ObjectNode payload
) {}
```

`FormDraftResponse` mirrors every `DraftView` field. `FormDraftSummaryResponse` includes `id`, `contextKey`, `currentStep`, `updatedAt`, and `expiresAt`.

- [ ] **Step 4: Implement the controller**

Use `Authentication.getName()` as the owner key.

```java
@RestController
@RequestMapping("/api/form-drafts")
public class FormDraftController {
    @GetMapping("/{type}")
    public ResponseEntity<FormDraftResponse> get(
        @PathVariable FormDraftType type,
        @RequestParam String contextKey,
        Authentication authentication
    ) {
        return drafts.find(authentication.getName(), type, contextKey)
            .map(FormDraftResponse::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{type}")
    public FormDraftResponse save(
        @PathVariable FormDraftType type,
        @RequestBody SaveFormDraftRequest request,
        Authentication authentication
    ) {
        return FormDraftResponse.from(drafts.save(
            authentication.getName(),
            request.toCommand(type)
        ));
    }
}
```

Implement delete and obligation list with the same owner lookup. Do not accept `ownerId` or username from the request body.

- [ ] **Step 5: Implement JSON exception responses**

Use `@RestControllerAdvice(assignableTypes = FormDraftController.class)`.

Response shapes:

```json
{"message":"Campo de rascunho não permitido: calculatedTotal"}
```

```json
{
  "message":"Este rascunho foi alterado em outro dispositivo.",
  "current":{ "...":"current FormDraftResponse" }
}
```

Map validation to 400, missing resource to 404, and `FormDraftConflictException` to 409. This prevents the existing HTML `GlobalExceptionHandler` from rendering an error page for draft fetches.

- [ ] **Step 6: Add CSRF metadata to the shared head**

```html
<meta name="_csrf" th:content="${_csrf != null ? _csrf.token : ''}">
<meta name="_csrf_header" th:content="${_csrf != null ? _csrf.headerName : ''}">
```

Place the tags after the viewport meta and before CSS links.

- [ ] **Step 7: Run focused tests**

Run:

```bash
./mvnw -Dtest=FormDraftApiTest test
```

Expected: all API authentication, CSRF, validation, conflict, and ownership tests pass.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/draft/web \
        src/main/resources/templates/fragments/head.html \
        src/test/java/dev/harrison/rendacomcarro/draft/FormDraftApiTest.java
git commit -m "feat: expose secure form draft api"
```

---

### Task 5: Build shared localized inputs and guided-form frontend infrastructure

**Files:**
- Create: `src/main/java/dev/harrison/rendacomcarro/shared/web/FlexibleBigDecimalEditor.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/shared/web/WebBindingAdvice.java`
- Create: `src/main/resources/templates/fragments/guided-form.html`
- Modify: `src/main/resources/static/css/app.css`
- Create: `src/main/resources/static/js/form-draft-client.js`
- Create: `src/main/resources/static/js/guided-form-state.js`
- Create: `src/main/resources/static/js/guided-form.js`
- Create: `package.json`
- Create: `src/test/java/dev/harrison/rendacomcarro/web/FlexibleBigDecimalEditorTest.java`
- Create: `src/test/js/guided-form-state.test.mjs`
- Create: `src/test/js/form-draft-client.test.mjs`
- Modify: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: the API from Task 4 and form metadata rendered as `data-*` attributes.
- Produces:
  - `parseFlexibleDecimal(String): BigDecimal`
  - `FormDraftClient.load/save/discard/list`
  - `GuidedFormController`
  - reusable Thymeleaf fragments `draft-status`, `recovery-modal`, `conflict-modal`, and `mobile-actions`.

- [ ] **Step 1: Write failing Java decimal parser tests**

```java
@Test
void acceptsBrazilianAndTechnicalDecimalFormats() {
    FlexibleBigDecimalEditor editor = new FlexibleBigDecimalEditor();

    editor.setAsText("1.234,56");
    assertThat(editor.getValue()).isEqualTo(new BigDecimal("1234.56"));

    editor.setAsText("1234.56");
    assertThat(editor.getValue()).isEqualTo(new BigDecimal("1234.56"));

    editor.setAsText("75");
    assertThat(editor.getValue()).isEqualTo(new BigDecimal("75"));
}
```

Also test blank -> `null`, `-1,00`, and invalid text -> `IllegalArgumentException` with a friendly message.

- [ ] **Step 2: Write failing dependency-free JavaScript tests**

`guided-form-state.test.mjs` must cover:

```javascript
assert.equal(nextStep(1, 4), 2);
assert.equal(previousStep(1), 1);
assert.equal(clampStep(8, 4), 4);
assert.equal(isMobileWizard(767), true);
assert.equal(isMobileWizard(768), false);
assert.equal(draftStorageKey("EXPENSE", "current"), "renda:draft:EXPENSE:current");
```

`form-draft-client.test.mjs` uses a fake `fetch` and fake storage to verify:

- CSRF header is included on PUT/DELETE;
- save sends `version` and `force`;
- network failure writes the unsynchronized payload to local storage;
- successful save removes the emergency local copy;
- HTTP 409 throws a `DraftConflictError` containing `current`.

- [ ] **Step 3: Run tests and verify failures**

Run:

```bash
./mvnw -Dtest=FlexibleBigDecimalEditorTest test
node --test src/test/js/*.test.mjs
```

Expected: Java compilation fails and Node reports missing modules.

- [ ] **Step 4: Implement flexible decimal binding**

The editor delegates parsing to `FlexibleDecimalParser.parse` and must:

1. trim whitespace;
2. remove non-breaking spaces;
3. when both `.` and `,` exist, remove dots and use comma as decimal separator;
4. when only comma exists, replace comma with dot;
5. otherwise parse the dot notation unchanged;
6. render the value with comma in `getAsText()`.

Register globally:

```java
@ControllerAdvice
public class WebBindingAdvice {
    @InitBinder
    void bindFlexibleDecimals(WebDataBinder binder) {
        binder.registerCustomEditor(
            BigDecimal.class,
            new FlexibleBigDecimalEditor()
        );
    }
}
```

All decimal form controls changed by later tasks use `type="text"` plus `inputmode="decimal"` so comma input remains valid without JavaScript.

- [ ] **Step 5: Implement pure state helpers and the API client**

`guided-form-state.js` exports:

```javascript
export const MOBILE_BREAKPOINT = 768;
export function clampStep(step, maxStep) { ... }
export function nextStep(step, maxStep) { ... }
export function previousStep(step) { ... }
export function isMobileWizard(width) { ... }
export function draftStorageKey(type, contextKey) { ... }
export function serializeEditableFields(form) { ... }
```

`serializeEditableFields`:

- ignores buttons and fields with `data-draft-ignore`;
- includes checked checkboxes/radios only;
- returns strings, not parsed totals;
- does not include disabled/read-only calculated output unless the field has `data-draft-include`;
- never reads file inputs.

`FormDraftClient` constructor accepts injected `fetchImpl` and `storage` for tests. Production defaults to `window.fetch` and `window.localStorage`.

Use `fetch(..., {credentials: "same-origin"})` and the CSRF metadata from Task 4. Use `fetch` with `keepalive: true` during `pagehide`; do not use `sendBeacon`, because the CSRF header must be preserved.

- [ ] **Step 6: Implement the guided controller**

Required behavior:

- JavaScript adds `.guided-form--enhanced`; CSS hides steps only after this class exists.
- On desktop, all `[data-form-step]` sections remain visible.
- Below 768 px, only `data-step=currentStep` is visible.
- `Continue`:
  1. calls `reportValidity()` on controls in the current step;
  2. focuses the first invalid control;
  3. immediately saves the draft;
  4. advances only after save succeeds.
- Normal changes debounce save by exactly 1,500 ms.
- `[data-save-on-blur]` saves immediately on blur.
- Save status uses `aria-live="polite"` and exact text: `Salvando…`, `Rascunho salvo às HH:mm`, `Falha ao salvar — tentar novamente`.
- Recovery modal offers `Continuar rascunho` and `Começar novamente`.
- Conflict modal offers `Usar versão mais recente`, `Revisar minhas alterações`, and `Substituir versão do servidor`.
- Restored strings are assigned only through `.value`, `.checked`, or `.textContent`.
- `pagehide` attempts a keepalive save and writes a local emergency copy first.
- On reconnect, local and server versions are compared before any upload.

Expose module hooks:

```javascript
document.dispatchEvent(new CustomEvent("guided-form:restored", {detail}));
document.dispatchEvent(new CustomEvent("guided-form:before-save", {detail}));
document.dispatchEvent(new CustomEvent("guided-form:step-changed", {detail}));
```

- [ ] **Step 7: Add reusable Thymeleaf fragments and CSS**

Fragments must use Bootstrap modal markup and stable IDs scoped by a caller-provided prefix.

CSS requirements:

```css
.guided-step { scroll-margin-top: 1rem; }
.guided-calculated { background: var(--bs-tertiary-bg); }
.guided-save-status { min-height: 1.5rem; }
.form-control:focus, .form-select:focus, .btn:focus-visible {
  outline: 3px solid rgba(13, 110, 253, .25);
  outline-offset: 1px;
}
```

At widths below 768 px:

- `.guided-form--enhanced [data-form-step][hidden] { display: none !important; }`
- sticky mobile action bar at the bottom;
- buttons and controls have minimum height 44 px;
- add bottom padding so the sticky bar does not cover content.

At 768 px and above, force all steps visible and hide the mobile progress/action controls.

- [ ] **Step 8: Add the Node test command and CI step**

`package.json`:

```json
{
  "private": true,
  "type": "module",
  "scripts": {
    "test:js": "node --test src/test/js/*.test.mjs"
  }
}
```

In CI, add `actions/setup-node@v4` with Node `22`, then run:

```yaml
- name: Run JavaScript tests
  run: npm run test:js
```

Place it before Maven tests.

- [ ] **Step 9: Run all focused shared tests**

Run:

```bash
./mvnw -Dtest=FlexibleBigDecimalEditorTest test
npm run test:js
```

Expected: Java decimal tests and all Node tests pass.

- [ ] **Step 10: Commit**

```bash
git add package.json .github/workflows/ci.yml \
        src/main/java/dev/harrison/rendacomcarro/shared/web \
        src/main/resources/templates/fragments \
        src/main/resources/static/css/app.css \
        src/main/resources/static/js \
        src/test/java/dev/harrison/rendacomcarro/web/FlexibleBigDecimalEditorTest.java \
        src/test/js
git commit -m "feat: add guided form and localized input foundation"
```

---

### Task 6: Integrate guided drafts into the expense form

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseForm.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/web/ExpenseController.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/expense/application/ExpenseFormSubmissionService.java`
- Modify: `src/main/resources/templates/expenses/form.html`
- Modify: `src/main/resources/static/js/expense-form.js`
- Modify: `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseWebTest.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/expense/ExpenseFormSubmissionServiceTest.java`

**Interfaces:**
- Consumes: generic draft API/frontend and `ExpenseService`.
- Produces:

```java
YearMonth ExpenseForm.getCompetenceMonth()
BigDecimal ExpenseForm.getProfessionalPercentagePercent()
BigDecimal ExpenseForm.professionalPercentageRatio()
Expense ExpenseFormSubmissionService.submit(String username, ExpenseForm form)
```

- [ ] **Step 1: Add failing web tests for the new input contract**

Verify the rendered page includes:

- `data-guided-form`;
- `data-draft-type="EXPENSE"`;
- `data-draft-context-key="current"`;
- three numbered sections;
- `R$` prefix;
- competence input `type="month"`;
- percent suffix `%`;
- specific placeholders (`Selecione um veículo`, `Selecione uma categoria`);
- `aria-describedby` on required inputs;
- module and generic scripts loaded as `type="module"`.

POST:

```java
.param("competenceMonth", "2026-07")
.param("professionalPercentagePercent", "75")
```

Then assert the persisted expense has competence date `2026-07-01` and internal professional percentage `0.7500`.

- [ ] **Step 2: Add a failing transactional draft-deletion test**

Seed the owner’s `EXPENSE/current` draft, submit a valid expense through `ExpenseFormSubmissionService`, and assert:

```java
assertThat(expenses.count()).isEqualTo(1);
assertThat(drafts.find("expense-owner", FormDraftType.EXPENSE, "current"))
    .isEmpty();
```

Force an invalid domain command and assert the draft remains.

- [ ] **Step 3: Run the focused tests and verify failure**

Run:

```bash
./mvnw -Dtest=ExpenseWebTest,ExpenseFormSubmissionServiceTest test
```

Expected: assertions fail because the old fields, markup, and submission coordinator are still present.

- [ ] **Step 4: Update the web form model**

Replace `LocalDate competenceDate` with:

```java
@NotNull
@DateTimeFormat(pattern = "yyyy-MM")
private YearMonth competenceMonth = YearMonth.now();
```

Replace `professionalPercentage` with:

```java
@DecimalMin("0")
@DecimalMax("100")
private BigDecimal professionalPercentagePercent;

public BigDecimal professionalPercentageRatio() {
    return professionalPercentagePercent == null
        ? null
        : professionalPercentagePercent.movePointLeft(2);
}
```

Keep the domain and database representation unchanged.

- [ ] **Step 5: Add the transactional submission coordinator**

```java
@Service
public class ExpenseFormSubmissionService {
    private final ExpenseService expenses;
    private final FormDraftService drafts;

    @Transactional
    public Expense submit(String username, ExpenseForm form) {
        Expense created = expenses.create(new ExpenseService.CreateExpenseCommand(
            form.getVehicleId(),
            form.getOperationalDayId(),
            form.getShiftId(),
            form.getCategoryId(),
            form.getExpenseDate(),
            form.getCompetenceMonth().atDay(1),
            form.getPaidDate(),
            form.getAmount(),
            form.getClassification(),
            form.getAllocationMethod(),
            form.professionalPercentageRatio(),
            form.getProfessionalFixedAmount(),
            form.getAdjustmentReason(),
            form.getNotes()
        ));
        drafts.complete(username, FormDraftType.EXPENSE, "current");
        return created;
    }
}
```

The controller accepts `Authentication`, calls this coordinator, and repopulates categories and vehicles on validation failure.

- [ ] **Step 6: Rebuild the expense template as three guided sections**

Section 1 — `Dados do gasto`:

- vehicle, category, amount with `R$`, expense date.

Section 2 — `Classificação e competência`:

- classification;
- competence month;
- optional paid date with help `Deixe vazio enquanto estiver pendente`;
- allocation controls shown only for `MIXED`;
- percent field uses 0–100 and `%`;
- fixed amount uses `R$`;
- manual reason appears only for manual percentage/fixed amount.

Section 3 — `Revisão e observações`:

- live summary showing total and amount attributed to the operation;
- notes;
- final submit.

Use `type="text" inputmode="decimal"` for money and percent. Mark calculated summary elements with `data-summary-*`; do not submit them.

- [ ] **Step 7: Update expense JavaScript as an ES module**

Keep conditional rate controls, but change percent handling to 0–100. Before each draft save and summary refresh:

```javascript
const ratio = percentValue === null ? null : percentValue / 100;
```

Dispatch or listen to `guided-form:restored` so restored classification and allocation values immediately refresh conditional controls and summary.

- [ ] **Step 8: Run focused tests**

Run:

```bash
./mvnw -Dtest=ExpenseWebTest,ExpenseFormSubmissionServiceTest test
npm run test:js
```

Expected: markup, localized inputs, ratio conversion, atomic draft deletion, and existing expense normalization tests pass.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/expense \
        src/main/resources/templates/expenses/form.html \
        src/main/resources/static/js/expense-form.js \
        src/test/java/dev/harrison/rendacomcarro/expense
git commit -m "feat: guide and autosave expense entry"
```

---

### Task 7: Integrate guided drafts into mileage closing

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/web/MonthlyOdometerClosingForm.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/expense/web/MonthlyOdometerClosingController.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/expense/application/MonthlyClosingFormSubmissionService.java`
- Modify: `src/main/resources/templates/mileage-closings/form.html`
- Modify: `src/main/resources/static/js/mileage-closing-form.js`
- Modify: `src/test/java/dev/harrison/rendacomcarro/expense/MonthlyMileageClosingWebTest.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/expense/MonthlyClosingFormSubmissionServiceTest.java`

**Interfaces:**
- Consumes: `MonthlyOdometerClosingService.preview/confirm` and generic drafts.
- Produces:

```java
String MonthlyOdometerClosingForm.draftContextKey()
MonthlyOdometerClosing MonthlyClosingFormSubmissionService.submit(
    String username,
    MonthlyOdometerClosingForm form
)
```

- [ ] **Step 1: Add failing tests for guided markup and context key**

For vehicle `V` and month `2026-07`, assert the preview page contains:

```text
data-draft-type="MILEAGE_CLOSING"
data-draft-context-key="vehicle:V:month:2026-07"
```

Also assert:

- three sections;
- `km` suffixes;
- automatic values are read-only;
- `Corrigir valor` exposes manual fields;
- blocking previews do not render the final submit button;
- no saved draft value is trusted as the preview’s automatic total.

- [ ] **Step 2: Add a failing atomic submission test**

Seed a mileage draft using the exact vehicle/month key. Submit a valid confirmation and assert closing creation and draft deletion. Make confirmation fail with a blocking open day and assert the draft remains.

- [ ] **Step 3: Run focused tests and verify failure**

Run:

```bash
./mvnw -Dtest=MonthlyMileageClosingWebTest,MonthlyClosingFormSubmissionServiceTest test
```

Expected: guided markup and coordinator assertions fail.

- [ ] **Step 4: Add a canonical context-key method**

```java
public String draftContextKey() {
    if (vehicleId == null || month == null) {
        return null;
    }
    return "vehicle:" + vehicleId + ":month:" + month;
}
```

Do not store inferred origin labels, counts, totals, personal kilometers, or percentage in the draft.

- [ ] **Step 5: Add the transactional submission coordinator**

```java
@Service
public class MonthlyClosingFormSubmissionService {
    private final MonthlyOdometerClosingService closings;
    private final FormDraftService drafts;

    @Transactional
    public MonthlyOdometerClosing submit(
        String username,
        MonthlyOdometerClosingForm form
    ) {
        MonthlyOdometerClosing closing = closings.confirm(
            new MonthlyOdometerClosingService.ConfirmCommand(
                form.getVehicleId(),
                form.getMonth(),
                form.isManualAdjustment(),
                form.getInitialOdometer(),
                form.getFinalOdometer(),
                form.getProfessionalKilometers(),
                form.getAdjustmentReason(),
                form.isConfirmWarnings()
            )
        );
        drafts.complete(
            username,
            FormDraftType.MILEAGE_CLOSING,
            form.draftContextKey()
        );
        return closing;
    }
}
```

The controller still recalculates preview on GET and POST. It never fills automatic values from a draft.

- [ ] **Step 6: Rebuild the page as the approved guided flow**

Section 1 — `Veículo e mês`:

- keep GET-based preview selection;
- on mobile, `Continuar` submits the GET form and reloads the page with vehicle/month;
- specific vehicle placeholder and month input.

Section 2 — `Prévia calculada`:

- origins, counts, alerts, inferred odometers, total, professional, personal, and percentage;
- all calculated values visually distinct and read-only.

Section 3 — `Revisão e confirmação`:

- warnings acknowledgment;
- correction button;
- manual fields and mandatory reason only after correction;
- final submit.

The guided draft attributes are rendered only when vehicle, month, and preview exist.

- [ ] **Step 7: Update module JavaScript**

On `guided-form:before-save`:

- when `manualAdjustment=false`, remove odometer and reason fields from the outgoing draft payload;
- when `true`, include the three corrected values and reason;
- keep `confirmWarnings`;
- recalculate display cards locally for manual edits, but label them `Prévia da correção`; the server remains authoritative.

On restore:

- always keep the newly calculated server preview;
- only apply saved odometer fields when restored payload has `manualAdjustment=true`;
- then enable correction mode and show the reason.

- [ ] **Step 8: Run focused tests**

Run:

```bash
./mvnw -Dtest=MonthlyMileageClosingWebTest,MonthlyClosingFormSubmissionServiceTest test
npm run test:js
```

Expected: automatic inference remains authoritative, guided markup is present, and draft deletion is transactional.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/expense \
        src/main/resources/templates/mileage-closings/form.html \
        src/main/resources/static/js/mileage-closing-form.js \
        src/test/java/dev/harrison/rendacomcarro/expense
git commit -m "feat: guide and autosave mileage closing"
```

---

### Task 8: Integrate guided drafts into monthly goals

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/web/GoalForm.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/goal/web/GoalController.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/goal/application/GoalFormSubmissionService.java`
- Modify: `src/main/resources/templates/goals/form.html`
- Create: `src/main/resources/static/js/goal-form.js`
- Modify: `src/test/java/dev/harrison/rendacomcarro/goal/GoalServiceTest.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/goal/GoalWebTest.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/goal/GoalFormSubmissionServiceTest.java`

**Interfaces:**
- Consumes: `GoalService` and generic drafts.
- Produces:

```java
Set<LocalDate> GoalForm.parsePlannedDates()
String GoalForm.draftContextKey()
MonthlyGoal GoalFormSubmissionService.submit(String username, GoalForm form)
```

- [ ] **Step 1: Add failing form parsing tests**

Test:

- commas, semicolons, spaces, and line breaks;
- duplicate dates collapse into one;
- invalid ISO date gives `DomainValidationException`;
- date outside selected month is rejected;
- Sunday is rejected;
- key is `month:2026-07`.

- [ ] **Step 2: Add failing web and submission tests**

Assert the form has:

- three guided sections;
- `R$` prefixes;
- month input;
- decimal planned-hours input;
- date picker enhancer;
- textarea fallback for no JavaScript;
- `data-draft-type="MONTHLY_GOAL"`;
- key derived from the selected month.

Seed a goal draft, submit successfully, and assert atomic deletion. Submit a duplicate month and assert the draft remains.

- [ ] **Step 3: Run focused tests and verify failure**

Run:

```bash
./mvnw -Dtest=GoalServiceTest,GoalWebTest,GoalFormSubmissionServiceTest test
```

Expected: missing parser, guided markup, and coordinator failures.

- [ ] **Step 4: Move planned-date parsing into GoalForm**

```java
public Set<LocalDate> parsePlannedDates() {
    try {
        return Arrays.stream(plannedDates.split("[,;\\s]+"))
            .filter(value -> !value.isBlank())
            .map(LocalDate::parse)
            .peek(this::validatePlannedDate)
            .collect(Collectors.toCollection(TreeSet::new));
    } catch (DateTimeParseException exception) {
        throw new DomainValidationException(
            "Use datas no formato AAAA-MM-DD."
        );
    }
}

private void validatePlannedDate(LocalDate date) {
    if (!YearMonth.from(date).equals(month)) {
        throw new DomainValidationException(
            "Todos os dias planejados devem pertencer ao mês da meta."
        );
    }
    if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
        throw new DomainValidationException(
            "Domingos não podem ser adicionados aos dias planejados."
        );
    }
}
```

- [ ] **Step 5: Add transactional submission**

Create the goal through `GoalService.create(...)`, then complete `MONTHLY_GOAL/month:YYYY-MM` inside the same transaction.

The controller catches `DomainValidationException`, rejects `plannedDates`, and returns the populated form.

- [ ] **Step 6: Rebuild the goal template**

Section 1 — `Mês e valores`:

- month;
- personal net goal with `R$`;
- operational goal with `R$`.

Section 2 — `Planejamento de trabalho`:

- planned hours;
- one date input plus `Adicionar dia`;
- selected-date chips with remove buttons;
- original `plannedDates` textarea remains in the HTML for progressive fallback and is hidden only after JS enhancement.

Section 3 — `Revisão`:

- number of planned days;
- total planned hours;
- target per planned day;
- final submit.

- [ ] **Step 7: Implement goal-form.js**

- Parse the textarea into a sorted unique list.
- Reject Sunday and dates outside the selected month before adding.
- Keep the textarea synchronized as comma-separated ISO dates.
- Recompute summary on every change.
- Respond to `guided-form:restored` by rebuilding chips and summary.
- Use `.textContent` for chip labels.

- [ ] **Step 8: Run focused tests**

Run:

```bash
./mvnw -Dtest=GoalServiceTest,GoalWebTest,GoalFormSubmissionServiceTest test
npm run test:js
```

Expected: parsing, guided markup, summaries’ source fields, and atomic draft behavior pass.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/goal \
        src/main/resources/templates/goals/form.html \
        src/main/resources/static/js/goal-form.js \
        src/test/java/dev/harrison/rendacomcarro/goal
git commit -m "feat: guide and autosave monthly goals"
```

---

### Task 9: Integrate guided drafts into financial obligations

**Files:**
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/web/ObligationForm.java`
- Modify: `src/main/java/dev/harrison/rendacomcarro/finance/web/FinancialObligationController.java`
- Create: `src/main/java/dev/harrison/rendacomcarro/finance/application/ObligationFormSubmissionService.java`
- Modify: `src/main/resources/templates/obligations/form.html`
- Modify: `src/main/resources/templates/obligations/list.html`
- Create: `src/main/resources/static/js/obligation-form.js`
- Create: `src/test/java/dev/harrison/rendacomcarro/finance/FinancialObligationWebTest.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/finance/ObligationFormSubmissionServiceTest.java`
- Modify: `src/test/java/dev/harrison/rendacomcarro/finance/InstallmentScheduleCalculatorTest.java`

**Interfaces:**
- Consumes: `FinancialObligationService`, draft service, and obligation definitions.
- Produces:

```java
BigDecimal ObligationForm.annualRateRatio()
String ObligationForm.getDraftKey()
FinancialObligation ObligationFormSubmissionService.submit(
    String username,
    ObligationForm form
)
```

- [ ] **Step 1: Add failing web tests**

Assert:

- GET `/obligations/new` generates `draft:{uuid}`;
- the form has four guided sections;
- principal, installment, and monthly target use `R$`;
- annual rate accepts 0–100 and shows `% ao ano`;
- mode choices use labeled radio cards rather than a generic select;
- irrelevant structured/flexible fields are conditionally hidden;
- obligation list shows active draft cards with `Continuar` links;
- multiple obligation drafts can coexist.

POST `annualRatePercent=12` and assert the persisted annual rate is `0.12`.

- [ ] **Step 2: Add failing atomic submission tests**

Seed `OBLIGATION/draft:{uuid}`, submit a valid flexible obligation, and assert both record creation and draft deletion. Submit structured mode without first due date and assert no record is created and the draft remains.

- [ ] **Step 3: Run focused tests and verify failure**

Run:

```bash
./mvnw -Dtest=FinancialObligationWebTest,ObligationFormSubmissionServiceTest,InstallmentScheduleCalculatorTest test
```

Expected: generated key, guided markup, percent mapping, and draft behavior fail.

- [ ] **Step 4: Update the form model**

Add:

```java
@DecimalMin("0")
@DecimalMax("100")
private BigDecimal annualRatePercent = BigDecimal.ZERO;

@NotBlank
private String draftKey;

public BigDecimal annualRateRatio() {
    return annualRatePercent == null
        ? BigDecimal.ZERO
        : annualRatePercent.movePointLeft(2);
}
```

Remove direct binding of the internal `annualRate`.

Add a class-level validation method or controller validation:

- structured mode requires first due date and term months > 0;
- flexible mode requires monthly target > 0;
- planned installment is optional but non-negative;
- first due date cannot precede start date.

- [ ] **Step 5: Generate and preserve obligation draft keys**

GET behavior:

```java
String draftKey = requestedDraftKey;
if (draftKey == null || draftKey.isBlank()) {
    draftKey = "draft:" + UUID.randomUUID();
}
form.setDraftKey(draftKey);
```

Only accept keys validated by `ObligationDraftDefinition`. On list, call `drafts.listActive(username, FormDraftType.OBLIGATION)` and render the summaries.

- [ ] **Step 6: Add transactional submission**

Map `annualRatePercent` to `annualRateRatio()`, create through `FinancialObligationService`, then complete the exact `draftKey` inside the same transaction.

- [ ] **Step 7: Rebuild the form as four guided sections**

Section 1 — `Identificação`:

- creditor;
- type;
- optional vehicle, displayed with name, plate, and current odometer.

Section 2 — `Valor e modalidade`:

- principal with `R$`;
- mode radio cards;
- annual rate with `% ao ano`;
- start date.

Section 3 — `Plano de pagamento`:

- structured: first due date, term months, planned installment;
- flexible: monthly target;
- contextual explanation shown only for the chosen mode.

Section 4 — `Revisão`:

- creditor, principal, mode, annual rate, estimated monthly commitment;
- notes;
- final submit.

- [ ] **Step 8: Implement obligation-form.js**

- Toggle structured/flexible controls and their `required` attributes.
- Clear irrelevant values only after explicit mode change, not on initial recovery.
- Calculate an informational installment preview using the same formula as `InstallmentScheduleCalculator`; label it `Estimativa — o servidor recalcula ao salvar`.
- Refresh after `guided-form:restored`.
- Never submit the preview value as authoritative.

- [ ] **Step 9: Run focused tests**

Run:

```bash
./mvnw -Dtest=FinancialObligationWebTest,ObligationFormSubmissionServiceTest,InstallmentScheduleCalculatorTest test
npm run test:js
```

Expected: multiple drafts, conditional form, annual-rate mapping, and atomic completion pass.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/dev/harrison/rendacomcarro/finance \
        src/main/resources/templates/obligations \
        src/main/resources/static/js/obligation-form.js \
        src/test/java/dev/harrison/rendacomcarro/finance
git commit -m "feat: guide and autosave financial obligations"
```

---

### Task 10: Complete cross-form acceptance, documentation, and full verification

**Files:**
- Modify: `src/test/java/dev/harrison/rendacomcarro/AcceptanceFlowTest.java`
- Create: `src/test/java/dev/harrison/rendacomcarro/web/GuidedFormsWebContractTest.java`
- Modify: `README.md`
- Modify: `docs/architecture.md`
- Modify: `docs/mvp-acceptance-checklist.md`
- Modify: `docs/superpowers/specs/2026-07-13-guided-forms-and-synced-drafts-design.md`

**Interfaces:**
- Consumes: every task above.
- Produces: end-to-end regression coverage, operator documentation, and the final reviewable branch.

- [ ] **Step 1: Add a failing cross-form web contract test**

For each of the four form URLs, assert:

- page is authenticated;
- guided form attributes exist;
- all fields remain present in server HTML;
- required fields have associated labels;
- errors use `aria-describedby`;
- shared recovery/conflict/status fragments exist;
- generic guided module is loaded;
- no technical enum constant appears in visible option text;
- desktop sections are not server-hidden.

- [ ] **Step 2: Extend AcceptanceFlowTest**

Add one coherent scenario:

1. create an expense draft through the JSON API;
2. recover it;
3. submit the final expense and verify draft removal;
4. create a mileage draft with manual correction values;
5. confirm closing and verify server recalculation/audit;
6. create a goal draft and final goal;
7. create two obligation drafts, complete one, and verify the other remains;
8. simulate a stale update and assert HTTP 409;
9. set a draft expiration in the past and run cleanup;
10. verify an invalid final submission leaves its draft intact.

- [ ] **Step 3: Run the new contract and acceptance tests**

Run:

```bash
./mvnw -Dtest=GuidedFormsWebContractTest,AcceptanceFlowTest test
```

Expected before documentation/final fixes: any uncovered markup or lifecycle gaps fail explicitly.

- [ ] **Step 4: Update documentation**

README:

- explain guided desktop/mobile behavior;
- autosave and seven-day expiration;
- recovery choice;
- cross-device synchronization and conflict prompt;
- no-JavaScript fallback.

Architecture:

- add the `draft` module;
- show owner/type/context uniqueness;
- document JSON definition registry;
- document final transactional deletion;
- document local emergency cache as non-authoritative;
- document daily cleanup.

Checklist:

- desktop width at least 1280 px: all sections visible;
- mobile widths 360 px and 412 px: one step visible and sticky actions do not obscure fields;
- reload recovery choice;
- computer-to-phone continuation;
- offline edit then reconnect;
- conflict between devices;
- seven-day expiration with test clock or database fixture;
- keyboard-only navigation;
- screen-reader labels/error association;
- no-JavaScript final submission;
- Raspberry/Tailscale real-device verification.

Change the design document status to:

```text
Status: aprovado; plano de implementação criado
```

- [ ] **Step 5: Run all automated verification**

Run:

```bash
npm run test:js
./mvnw -B clean test
./mvnw -B -DskipTests package
docker compose config >/tmp/renda-compose.yaml
```

Expected:

- Node test runner reports zero failures;
- Maven reports `BUILD SUCCESS`;
- package command succeeds;
- Compose renders without errors.

- [ ] **Step 6: Run local production-stack smoke verification**

Use non-production credentials:

```bash
cp .env.example .env
cp infra/backup/restic.env.example infra/backup/restic.env
docker compose up -d --build
curl --fail http://127.0.0.1:8080/actuator/health
docker compose down --remove-orphans
```

Expected: health endpoint returns `UP`, and all containers stop cleanly. Do not commit `.env` or `infra/backup/restic.env`.

- [ ] **Step 7: Commit final acceptance and docs**

```bash
git add README.md docs \
        src/test/java/dev/harrison/rendacomcarro/AcceptanceFlowTest.java \
        src/test/java/dev/harrison/rendacomcarro/web/GuidedFormsWebContractTest.java
git commit -m "test: verify guided forms and synced drafts"
```

- [ ] **Step 8: Push and open a pull request**

```bash
git push -u origin feature/guided-forms-synced-drafts
gh pr create \
  --base main \
  --head feature/guided-forms-synced-drafts \
  --title "feat: add guided forms and synchronized drafts" \
  --body-file docs/superpowers/plans/2026-07-13-guided-forms-and-synced-drafts.md
```

Do not merge. Confirm the GitHub Actions test and ops-smoke jobs are green before requesting review.

---

## Final Self-Review Checklist

- [ ] Every design requirement maps to a task.
- [ ] Expense has exactly one `current` draft.
- [ ] Mileage uses one vehicle/month draft.
- [ ] Goal uses one month draft.
- [ ] Obligations support multiple drafts and list them.
- [ ] Expiration is seven days and renewed on every valid update.
- [ ] Desktop and mobile behavior are separated by progressive enhancement.
- [ ] Autosave delay is 1,500 ms.
- [ ] Conflict resolution exposes all three approved actions.
- [ ] Local storage is emergency-only and never silently wins over the server.
- [ ] Derived values are recalculated on final submission.
- [ ] Draft deletion is transactional and failure preserves the draft.
- [ ] Unknown fields, oversized payloads, and binary attachments are rejected.
- [ ] Currency, percentage, month, date, and kilometer inputs follow the approved format.
- [ ] No-JavaScript submission remains possible.
- [ ] Tests cover domain, persistence, API, web contracts, JavaScript state, and end-to-end flows.
- [ ] Documentation and the manual mobile/device checklist are updated.
