# Shared Localized Inputs and Workload Planner Design

Date: 2026-07-14

## Context

The project already has input handling created for the vehicle form, including terminal-style money entry, odometer formatting, plate normalization, paste handling, deletion behavior, and whitespace normalization. The remaining guided forms still contain duplicated localized parsing and formatting logic, and several numeric fields rely on raw browser inputs.

The goal form also represents planned work only as a monthly decimal number of hours. That model is difficult to reason about because users commonly think in daily or weekly workloads, such as `8 h 30 min/dia` or `40 h/semana`.

This design expands PR #9 so that:

1. reusable localized input behavior is shared across vehicle, expense, goal, obligation, and mileage-closing forms;
2. the goal form replaces the raw planned-hours input with a workload planner supporting daily, weekly, and monthly entry;
3. the form preserves the original workload mode and value while deriving an authoritative total for the selected period;
4. all changes remain compatible with the serialized guided-draft saves introduced by PR #9.

## Goals

- Reuse one declarative implementation for money, percentage, odometer, and whitespace normalization.
- Preserve vehicle-only behavior, such as plate formatting, inside the vehicle module.
- Give users three workload entry modes: daily, weekly, and monthly.
- Accept hours and minutes in every workload mode.
- Calculate daily, weekly, and period totals from the selected planned dates.
- Preserve the original workload mode and entered duration when editing or restoring a draft.
- Use integer minutes internally to avoid decimal and rounding errors.
- Recalculate workload authoritatively on the server.
- Keep guided-form autosaves serialized and prevent formatting from creating duplicate requests.

## Non-goals

- Building a clock-style time picker.
- Treating planned workload as a time of day.
- Adding a third-party input-mask or date-calculation dependency.
- Reworking unrelated form layouts.
- Automatically merging PR #9.

## 1. Shared localized input behavior

### 1.1 Architecture

Generic formatters and DOM event handling will move out of vehicle-specific modules into shared modules with narrow responsibilities:

- a pure formatter/parser module for localized values;
- a DOM initializer that activates behavior through data attributes;
- the vehicle module retaining only vehicle-specific rules such as plate formatting and vehicle form state.

The initializer must be idempotent so that a field is never wired twice when a form is restored or a page module reinitializes a subtree.

### 1.2 Declarative attributes

The shared initializer will recognize the following attributes:

- `data-money-input`
- `data-percentage-input`
- `data-odometer-input`
- `data-normalize-spaces`

Optional configuration such as maximum digits or decimal scale may be represented by additional data attributes when a field needs a stricter domain limit.

### 1.3 Money behavior

Money uses terminal/card-reader entry with two implicit decimal places:

- `1` becomes `0,01`;
- `125` becomes `1,25`;
- `2399000` becomes `23.990,00`.

The behavior must support typing, paste, selection replacement, backspace, delete, and optional blank values. Prefixes such as `R$` remain outside the input in the Bootstrap/Tabler input group.

Target fields include:

- expense amount;
- expense professional fixed amount;
- goal personal net goal;
- goal operational goal;
- obligation principal;
- obligation planned installment;
- obligation monthly target;
- existing vehicle money fields.

### 1.4 Percentage behavior

Percentage also uses two implicit decimal places:

- `1` becomes `0,01`;
- `12` becomes `0,12`;
- `125` becomes `1,25`;
- `1250` becomes `12,50`.

The visible `%` remains outside the input. Values must remain within `0,00` and `100,00`; formatting shapes the value, while browser and backend validation report an out-of-range value rather than silently changing it.

Target fields include:

- expense professional percentage;
- obligation annual rate.

### 1.5 Odometer and distance behavior

Odometer fields use thousands grouping and at most one decimal place:

- `248351` becomes `248.351`;
- `248351,5` becomes `248.351,5`.

A decimal fraction appears only when the user enters one. Target fields include:

- vehicle odometer fields already using this behavior;
- mileage-closing initial odometer;
- mileage-closing final odometer;
- mileage-closing professional kilometers.

### 1.6 Integer fields

Counts remain plain non-negative integers and are not processed as money:

- obligation term in months;
- installment counts or similar count fields.

### 1.7 Text normalization

`data-normalize-spaces` applies to short single-line text fields and short reason fields. It collapses repeated spaces and removes leading and trailing spaces without changing letter case unless a field has a separate domain-specific rule.

Free-form notes and multiline observations preserve internal spaces and line breaks. They are only trimmed at the outer boundaries during submission.

### 1.8 Parsing and summaries

Form-specific scripts must import the shared localized parser instead of maintaining duplicate `parseDecimal` or money-formatting functions. Summaries therefore interpret the same displayed value that the backend binder receives.

### 1.9 Draft restoration

When guided drafts restore input values programmatically, the shared initializer reformats the restored values after the existing restoration event. Reformatting must not dispatch synthetic changes that create recursive or duplicate autosaves.

Real user edits continue to emit normal bubbling input/change events so the serialized save queue in PR #9 observes them.

## 2. Workload planner

### 2.1 User model

The existing raw planned-hours field is replaced by a workload component with three modes:

- `DAILY`: hours and minutes per selected day;
- `WEEKLY`: hours and minutes per calendar week;
- `MONTHLY`: hours and minutes for the complete selected period.

Every mode accepts separate non-negative hour and minute inputs. Minutes range from `0` to `59`.

Example values:

- `8 h 30 min/dia`;
- `40 h 00 min/semana`;
- `170 h 30 min/mês`.

This component is not implemented as a generic hour mask because its behavior depends on periodicity, planned dates, weekly grouping, proportional weeks, persistence, and domain calculations.

### 2.2 Displayed summary

The interface shows both the original workload and the calculated distribution. A weekly example is:

```text
Jornada informada
40 h/semana

Distribuição no período
Semana 1 · 2 dias · 16 h
Semana 2 · 5 dias · 40 h
Semana 3 · 4 dias · 40 h
Semana 4 · 6 dias · 40 h
Semana 5 · 4 dias · 32 h

Total planejado
168 h
```

The summary updates immediately after changes to mode, hours, minutes, or planned dates.

### 2.3 Calendar-week definition

A week runs from Monday through Sunday. Planned dates are deduplicated and sorted before calculation.

A calendar week fully contained within the goal period is an internal week. A calendar week intersecting the start or end boundary of the goal period without containing all seven period days is a boundary week.

### 2.4 Daily mode

The entered duration applies to every selected planned date:

```text
8 h 30 min/dia × 20 dias = 170 h no período
```

Weekly totals are the sum of the selected days inside each Monday-Sunday group.

### 2.5 Weekly mode: internal weeks

Each internal week receives the complete entered weekly duration, distributed evenly among that week's selected planned dates.

Examples for `40 h/semana`:

- 5 selected days: `8 h/dia`;
- 4 selected days: `10 h/dia`;
- 6 selected days: `6 h 40 min/dia`.

This deliberately calculates each week independently. It does not average all weeks into one fixed number of working days.

### 2.6 Weekly mode: recurring pattern

For boundary-week proration, the system infers the recurring working-day pattern from the weekday occurrences in internal weeks. It supports routines of 3, 4, 5, 6, or another observed number of days per week.

A weekday is part of the inferred pattern when it occurs as part of the recurring planned schedule across the available internal weeks. The implementation plan must define a deterministic inference algorithm and cover ties, but it must never assume Monday-Friday when sufficient date evidence exists.

Example:

```text
Padrão inferido: segunda a sexta
Semana inicial: somente quinta e sexta dentro do período
40 h × 2/5 = 16 h
```

### 2.7 Weekly mode: boundary weeks

A boundary week receives a proportional share of the entered weekly duration:

```text
weekly duration × selected recurring days present / recurring days in a full inferred week
```

The result is capped at the complete weekly duration, so selecting more days than the inferred baseline never creates more than one weekly workload.

### 2.8 Single partial week fallback

When the period contains only one partial week and there are no internal weeks from which to infer a recurring pattern, the system assumes a five-day reference.

For `40 h/semana`:

- 2 selected days produce `16 h`;
- 3 selected days produce `24 h`;
- 4 selected days produce `32 h`;
- 5 or more selected days produce the complete `40 h`.

The proportional result is then distributed among the selected dates in that week.

### 2.9 Weekly mode without dates

The entered weekly mode and duration remain valid draft data, but no calculated total is produced until planned dates exist. The interface displays:

> Selecione os dias planejados para calcular a distribuição.

The form does not assume five days provisionally and does not block entry before date selection.

### 2.10 Monthly mode

The entered monthly duration is the total workload for the selected period. It is divided evenly among all selected planned dates. Weekly totals are obtained by summing each week's allocated daily values.

The interface also derives equivalent average daily and weekly values for explanation, but the monthly duration remains the preserved original input.

### 2.11 Integer-minute calculations

All workload calculations use integer minutes. Persistence and domain calculations do not use floating-point hours.

When even distribution leaves remainder minutes, those minutes are assigned one at a time to the earliest selected dates in chronological order. This guarantees that the detailed distribution always sums exactly to the calculated total.

When proportional weekly arithmetic produces a fraction of a minute, the week total is rounded to the nearest minute once, before distribution among dates. The system must not round each day's fractional result independently.

### 2.12 Validation

- mode is required when a workload is supplied;
- hours cannot be negative;
- minutes must be between `0` and `59`;
- entered duration must be greater than zero for final submission;
- duplicate dates count once;
- no planned dates means the calculation is pending rather than zero;
- the server ignores any client-supplied calculated total and recalculates it;
- optional unrelated fields remain blank rather than being converted to zero.

## 3. Persistence and compatibility

### 3.1 Persisted source data

A goal preserves at least:

- workload periodicity: `DAILY`, `WEEKLY`, or `MONTHLY`;
- entered workload duration in integer minutes;
- calculated total duration for the selected period in integer minutes.

Example:

```text
periodicity = WEEKLY
enteredDurationMinutes = 2400
calculatedPeriodMinutes = 10080
```

### 3.2 Existing planned-hours compatibility

The existing decimal planned-hours value remains a derived compatibility representation of the authoritative calculated minutes where current reports or calculations still require it.

It is not the source used to restore the form. Editing a weekly goal must reopen as `40 h/semana`, not as the derived monthly total.

The implementation plan must identify every reader of the existing planned-hours field and migrate or adapt it without leaving two independent sources of truth.

### 3.3 Draft payload

Goal drafts store the user's source input:

- periodicity;
- entered hours;
- entered minutes;
- selected planned dates;
- normal guided-form metadata such as current step and optimistic-lock version.

A draft may include a preview total for display, but it is not authoritative. After restoration, the client recalculates the preview from source values.

### 3.4 Final submission

On final submission:

1. the server validates mode, duration, and dates;
2. the server recalculates the full distribution;
3. the server persists source data and authoritative calculated minutes;
4. compatibility decimal hours are derived from calculated minutes;
5. client-provided derived totals are ignored or compared only for diagnostic purposes.

## 4. Components and boundaries

### 4.1 Shared localized formatter module

Responsibilities:

- pure localized parsing;
- terminal-style money editing;
- percentage editing;
- odometer editing;
- whitespace normalization helpers.

It has no dependency on form IDs, guided drafts, or vehicle concepts.

### 4.2 Shared localized DOM module

Responsibilities:

- scan a document or form subtree for supported data attributes;
- attach idempotent beforeinput, input, paste, and blur handlers;
- preserve cursor/selection behavior;
- reformat restored values without emitting recursive autosaves.

### 4.3 Vehicle form module

Responsibilities:

- plate formatting;
- vehicle-only rules and state;
- invoking the shared initializer for generic vehicle fields.

It must not remain the owner of reusable money or odometer behavior.

### 4.4 Workload planner frontend module

Responsibilities:

- mode selection;
- hour/minute input coordination;
- immediate preview calculations;
- per-week and total summary rendering;
- reacting to planned-date changes;
- restoring source values from drafts;
- integrating with normal guided-form input events.

The client calculation mirrors the server rules for immediate feedback but is not authoritative.

### 4.5 Workload calculation domain service

Responsibilities:

- date deduplication and ordering;
- Monday-Sunday grouping;
- internal/boundary week classification;
- recurring weekday-pattern inference;
- five-day fallback for a single partial week;
- daily, weekly, and monthly calculation;
- minute rounding and remainder distribution;
- returning a structured result containing total and per-week/per-day distribution.

The service must be independent from controllers and template rendering so it can be thoroughly unit-tested.

## 5. Data flow

### 5.1 Initial render and edit

1. The server renders source workload mode and entered duration.
2. Shared localized inputs format all supported numeric values.
3. The workload planner calculates and renders a preview from the source workload and planned dates.
4. Existing goals therefore reopen in their original mode once migrated.

### 5.2 User edit

1. A user edits a masked field or workload value.
2. The responsible module updates the visible value and emits the normal user event.
3. The guided-form controller schedules the draft save through PR #9's serialized queue.
4. For workload changes, the preview recalculates before or alongside autosave without starting a second save path.

### 5.3 Draft restoration

1. The guided-form controller restores raw source values.
2. It emits the existing restoration event.
3. The localized-input module formats restored localized fields without synthetic autosave events.
4. The workload planner recalculates the preview from restored mode, duration, and dates.

### 5.4 Final save

1. The browser submits source values.
2. Backend binding parses localized numeric fields.
3. The workload domain service computes the authoritative distribution.
4. Validation errors return against the source fields the user can correct.
5. Valid source and derived values are persisted atomically.

## 6. Error handling

- Unsupported characters in localized numeric inputs are ignored without corrupting the prior valid value.
- Paste uses the same parser and limits as typing.
- Optional masked fields may be cleared completely.
- Out-of-range percentages, minutes, or domain values receive visible validation errors rather than silent clamping.
- A missing date set produces a pending-calculation state, not a false zero total.
- A draft conflict or save failure continues to use the guided-form error state already established by PR #9.
- Formatting and preview failures must not discard the user's source input.
- The workload service returns deterministic validation errors for invalid mode/duration/date combinations.

## 7. Testing strategy

### 7.1 Pure JavaScript formatter tests

- terminal money typing, paste, deletion, selection replacement, and blank values;
- percentage examples and 100-percent range handling;
- odometer grouping with optional one-decimal fraction;
- localized parsing shared by summaries;
- whitespace normalization;
- idempotent initialization.

### 7.2 Localized-input DOM tests

- each declarative attribute activates the intended behavior;
- restored values are reformatted;
- formatting does not emit duplicate autosave events;
- genuine edits still bubble to the guided-form controller;
- vehicle behavior remains unchanged after extraction.

### 7.3 Workload service unit tests

- daily, weekly, and monthly modes;
- hours plus minutes in every mode;
- internal weeks with 3, 4, 5, and 6 selected days;
- different day counts across adjacent internal weeks;
- partial start and end weeks;
- inferred recurring patterns other than five days;
- a single partial week using a five-day reference;
- no dates;
- duplicate and unordered dates;
- exact and fractional-minute proportional results;
- chronological remainder-minute distribution;
- totals exactly matching detailed distributions.

### 7.4 Workload frontend tests

- switching daily, weekly, and monthly modes;
- preserving source mode and duration;
- updating the preview after planned-date changes;
- displaying the no-dates instruction;
- restoring a draft as the original mode;
- creating only the expected queued autosaves.

### 7.5 Backend integration tests

- binding localized money, percentage, and odometer strings;
- persisting workload periodicity and entered minutes;
- server-side recalculation overriding a manipulated client total;
- editing existing and newly migrated goals;
- goal reports and calculations receiving compatible planned-hours values;
- validation messages for invalid hours, minutes, mode, and dates.

### 7.6 Regression and CI

- existing guided-draft race-condition tests remain green;
- expense, goal, obligation, mileage-closing, and vehicle form tests cover their new attributes;
- full JavaScript, JUnit, MockMvc, Testcontainers, packaging, credential, image, Compose, healthcheck, backup/restore, and shutdown workflows run before completion is claimed.

## 8. Migration and rollout

Existing goals containing only decimal planned hours require a deterministic migration policy. The implementation plan must inspect current data constraints and choose the least destructive representation. Unless existing metadata proves another original mode, migrated records should be represented as `MONTHLY`, with entered minutes derived from the old decimal value, because that preserves the previously stored meaning without inventing a daily or weekly schedule.

Database changes, application code, templates, JavaScript, and tests will remain within PR #9 as requested. The PR description must be updated after implementation to describe both the original save-serialization fix and the expanded input/workload work.

## 9. Acceptance criteria

The design is complete when all of the following are true:

- generic masks no longer depend on vehicle-named modules;
- money, percentage, and odometer fields across the scoped forms behave consistently;
- counts remain integers;
- the goal form supports daily, weekly, and monthly workload input with hours and minutes;
- weekly calculations operate independently by Monday-Sunday week;
- boundary weeks are prorated from inferred recurring weekdays;
- a lone partial week uses the approved five-day fallback;
- no-date workloads are preserved but marked pending;
- source mode and duration survive edit and draft restoration;
- backend integer-minute calculations are authoritative;
- derived totals always equal their detailed distribution;
- no formatting behavior bypasses or duplicates PR #9's serialized autosave queue;
- regression and full CI verification pass before the PR is considered ready.
