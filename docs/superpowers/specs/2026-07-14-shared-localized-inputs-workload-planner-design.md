# Shared Localized Inputs and Workload Planner Design

Date: 2026-07-14

## Context

The project already has input handling created for the vehicle form, including terminal-style money entry, odometer formatting, plate normalization, paste handling, deletion behavior, and whitespace normalization. The remaining guided forms still contain duplicated localized parsing and formatting logic, and several numeric fields rely on raw browser inputs.

The monthly goal form also represents planned work only as one decimal total of hours for the month. That model is difficult to reason about because users commonly think in daily or weekly workloads, such as `8 h 30 min/dia` or `40 h/semana`.

This design expands PR #9 so that:

1. reusable localized input behavior is shared across vehicle, expense, goal, obligation, and mileage-closing forms;
2. the goal form replaces the raw planned-hours input with a workload planner supporting daily, weekly, and monthly entry;
3. the form preserves the original workload mode and value while deriving an authoritative total for the selected month;
4. all changes remain compatible with the serialized guided-draft saves introduced by PR #9.

## Goals

- Reuse one declarative implementation for money, percentage, odometer, and whitespace normalization.
- Preserve vehicle-only behavior, such as plate formatting, inside the vehicle module.
- Give users three workload entry modes: daily, weekly, and monthly.
- Accept hours and minutes in every workload mode.
- Calculate daily, weekly, and monthly totals from the selected planned dates.
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
- `MONTHLY`: hours and minutes for the complete selected month.

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

Distribuição no mês
Semana 1 · 2 dias · 16 h
Semana 2 · 5 dias · 40 h
Semana 3 · 4 dias · 40 h
Semana 4 · 6 dias · 40 h
Semana 5 · 4 dias · 32 h

Total planejado
168 h
```

The summary updates immediately after changes to mode, hours, minutes, selected month, or planned dates.

### 2.3 Calendar-month and week definitions

The selected `month` field defines the calculation period from the first through the last calendar day of that month. A week runs from Monday through Sunday. Planned dates are deduplicated, sorted, and must belong to the selected month. Existing validation that rejects Sundays remains in effect.

A calendar week fully contained inside the selected month is an internal week. The first and last Monday-Sunday groups are boundary weeks when part of those groups falls outside the selected month.

### 2.4 Daily mode

The entered duration applies to every selected planned date:

```text
8 h 30 min/dia × 20 dias = 170 h no mês
```

Weekly totals are the sum of the selected days inside each Monday-Sunday group.

### 2.5 Weekly mode: internal weeks

Each internal week containing at least one selected planned date receives the complete entered weekly duration, distributed evenly among that week's selected dates. An internal week with no selected planned date contributes zero.

Examples for `40 h/semana`:

- 5 selected days: `8 h/dia`;
- 4 selected days: `10 h/dia`;
- 6 selected days: `6 h 40 min/dia`.

This deliberately calculates each week independently. It does not average all weeks into one fixed number of working days.

### 2.6 Weekly mode: deterministic recurring-pattern inference

Boundary-week proration needs a reference for the expected number of working days in a complete week. The reference is inferred only from non-empty internal weeks:

1. For each non-empty internal week, build the exact set of selected weekdays, such as `{MON, TUE, WED, THU, FRI}` or `{TUE, THU, SAT}`.
2. Count how often each exact weekday set occurs.
3. Use the most frequent exact set as the recurring pattern.
4. When two or more sets tie, use the tied set belonging to the internal week nearest to the boundary week being calculated.
5. If two tied candidate weeks have the same distance, use the earlier calendar week.
6. The expected full-week day count is the size of the chosen set.

The start and end boundary weeks may therefore resolve a tie differently when the schedule genuinely changes during the month. The chosen weekday set is shown in the explanatory summary so the proration is transparent.

Example:

```text
Padrão inferido: segunda a sexta, 5 dias
Semana inicial: 2 dias selecionados dentro do mês
40 h × 2/5 = 16 h
```

This algorithm supports recurring schedules of 3, 4, 5, 6, or another observed number of non-Sunday days and never assumes Monday-Friday when internal-week evidence exists.

### 2.7 Weekly mode: boundary weeks

A boundary week receives a proportional share of the entered weekly duration:

```text
weekly duration × min(selected dates in boundary week, expected full-week days)
                / expected full-week days
```

The numerator uses the actual number of selected dates inside the month. It is capped by the inferred expected day count, so selecting extra dates never creates more than one complete weekly workload in a boundary week.

Example for an inferred five-day pattern and `40 h/semana`:

- 2 selected dates: `16 h`;
- 4 selected dates: `32 h`;
- 5 or 6 selected dates: `40 h`.

### 2.8 Five-day fallback without internal-week evidence

When selected dates occur only in a boundary week, or no non-empty internal week exists from which to infer a pattern, the system assumes a five-day reference for that boundary calculation.

For `40 h/semana`:

- 2 selected days produce `16 h`;
- 3 selected days produce `24 h`;
- 4 selected days produce `32 h`;
- 5 or more selected days produce the complete `40 h`.

The proportional result is then distributed among the selected dates in that week.

### 2.9 Weekly mode without dates

The entered weekly mode and duration remain valid draft data, but no calculated total is produced until planned dates exist. The interface displays:

> Selecione os dias planejados para calcular a distribuição.

The form does not assume five days provisionally and does not block entry before date selection. The five-day fallback applies only after at least one planned date exists and a boundary week must be calculated without internal-week evidence.

### 2.10 Monthly mode

The entered monthly duration is the total workload for the selected month. It is divided evenly among all selected planned dates. Weekly totals are obtained by summing each week's allocated daily values.

The interface also derives equivalent average daily and weekly values for explanation, but the monthly duration remains the preserved original input.

### 2.11 Integer-minute calculations

All workload calculations use integer minutes. Persistence and domain calculations do not use floating-point hours.

When even distribution leaves remainder minutes, those minutes are assigned one at a time to the earliest selected dates in chronological order. This guarantees that the detailed distribution always sums exactly to the calculated total.

When proportional weekly arithmetic produces a fraction of a minute, the week total is rounded to the nearest minute once, before distribution among dates. Half-minute ties round upward. The system does not round each day's fractional result independently.

### 2.12 Validation

- mode is required when a workload is supplied;
- hours cannot be negative;
- minutes must be between `0` and `59`;
- entered duration must be greater than zero for final submission;
- planned dates must belong to the selected month;
- Sundays remain invalid planned dates;
- duplicate dates count once;
- no planned dates means the draft calculation is pending rather than zero;
- final goal submission still requires at least one valid planned date;
- the server ignores any client-supplied calculated total and recalculates it;
- optional unrelated fields remain blank rather than being converted to zero.

## 3. Persistence and compatibility

### 3.1 Persisted source data

A goal preserves:

- workload periodicity: `DAILY`, `WEEKLY`, or `MONTHLY`;
- entered workload duration in integer minutes;
- calculated total duration for the selected month in integer minutes.

Example:

```text
periodicity = WEEKLY
enteredDurationMinutes = 2400
calculatedMonthMinutes = 10080
```

A pending draft without planned dates has no authoritative calculated-month value. It must not persist an artificial zero.

### 3.2 Existing planned-hours compatibility

The existing decimal planned-hours value remains a derived compatibility representation of the authoritative calculated minutes where current reports or calculations still require it.

It is not the source used to restore the form. Editing a weekly goal must reopen as `40 h/semana`, not as the derived monthly total.

Every reader of the existing planned-hours field must either consume the authoritative minute total directly or consume the single derived decimal representation. No code path may independently edit both values.

### 3.3 Draft payload

Goal drafts store the user's source input:

- periodicity;
- entered hours;
- entered minutes;
- selected month;
- selected planned dates;
- normal guided-form metadata such as current step and optimistic-lock version.

A draft may include a preview total for display, but it is not authoritative. After restoration, the client recalculates the preview from source values.

### 3.4 Final submission

On final submission:

1. the server validates month, mode, duration, and dates;
2. the server recalculates the full monthly distribution;
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
- reacting to month and planned-date changes;
- restoring source values from drafts;
- integrating with normal guided-form input events.

The client calculation mirrors the server rules for immediate feedback but is not authoritative.

### 4.5 Workload calculation domain service

Responsibilities:

- month-boundary calculation;
- date validation, deduplication, and ordering;
- Monday-Sunday grouping;
- internal/boundary week classification;
- exact weekday-set frequency inference and deterministic tie-breaking;
- five-day fallback when internal-week evidence is absent;
- daily, weekly, and monthly calculation;
- minute rounding and remainder distribution;
- returning a structured result containing total and per-week/per-day distribution.

The service must be independent from controllers and template rendering so it can be thoroughly unit-tested.

## 5. Data flow

### 5.1 Initial render and edit

1. The server renders source workload mode and entered duration.
2. Shared localized inputs format all supported numeric values.
3. The workload planner calculates and renders a preview from the selected month, source workload, and planned dates.
4. Existing goals reopen in their persisted source mode after migration.

### 5.2 User edit

1. A user edits a masked field or workload value.
2. The responsible module updates the visible value and emits the normal user event.
3. The guided-form controller schedules the draft save through PR #9's serialized queue.
4. For workload changes, the preview recalculates without starting a second save path.

### 5.3 Draft restoration

1. The guided-form controller restores raw source values.
2. It emits the existing restoration event.
3. The localized-input module formats restored localized fields without synthetic autosave events.
4. The workload planner recalculates the preview from restored month, mode, duration, and dates.

### 5.4 Final save

1. The browser submits source values.
2. Backend binding parses localized numeric fields.
3. The workload domain service computes the authoritative monthly distribution.
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
- The workload service returns deterministic validation errors for invalid month/mode/duration/date combinations.

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
- internal weeks without selected dates;
- partial start and end weeks based on actual month boundaries;
- recurring exact weekday-set inference;
- deterministic tie resolution near each boundary;
- inferred recurring patterns other than five days;
- no internal-week evidence using a five-day reference;
- no dates;
- duplicate, unordered, out-of-month, and Sunday dates;
- exact and fractional-minute proportional results;
- half-minute rounding upward;
- chronological remainder-minute distribution;
- totals exactly matching detailed distributions.

### 7.4 Workload frontend tests

- switching daily, weekly, and monthly modes;
- preserving source mode and duration;
- updating the preview after month and planned-date changes;
- displaying the no-dates instruction;
- displaying the inferred weekday pattern used for a boundary;
- restoring a draft as the original mode;
- creating only the expected queued autosaves.

### 7.5 Backend integration tests

- binding localized money, percentage, and odometer strings;
- persisting workload periodicity and entered minutes;
- server-side recalculation overriding a manipulated client total;
- editing existing and newly migrated goals;
- goal reports and calculations receiving compatible planned-hours values;
- validation messages for invalid hours, minutes, mode, month, and dates.

### 7.6 Regression and CI

- existing guided-draft race-condition tests remain green;
- expense, goal, obligation, mileage-closing, and vehicle form tests cover their new attributes;
- full JavaScript, JUnit, MockMvc, Testcontainers, packaging, credential, image, Compose, healthcheck, backup/restore, and shutdown workflows run before completion is claimed.

## 8. Migration and rollout

Existing goals containing only decimal planned hours are migrated as `MONTHLY`, because the old field explicitly represented total hours available in the month. For each existing non-null value:

```text
enteredDurationMinutes = round(oldPlannedHours × 60)
calculatedMonthMinutes = enteredDurationMinutes
periodicity = MONTHLY
```

A null old value remains an incomplete/null workload rather than becoming zero. After migration, the compatibility decimal planned-hours value is derived from `calculatedMonthMinutes`; it is no longer an independently editable source.

Database changes, application code, templates, JavaScript, and tests remain within PR #9 as requested. The PR description must be updated after implementation to describe both the original save-serialization fix and the expanded input/workload work.

## 9. Acceptance criteria

The design is complete when all of the following are true:

- generic masks no longer depend on vehicle-named modules;
- money, percentage, and odometer fields across the scoped forms behave consistently;
- counts remain integers;
- the goal form supports daily, weekly, and monthly workload input with hours and minutes;
- the selected month is the authoritative calculation period;
- weekly calculations operate independently by Monday-Sunday week;
- internal weeks with selected dates receive the complete weekly duration;
- boundary weeks are prorated using deterministic recurring-pattern inference;
- absence of internal-week evidence uses the approved five-day fallback;
- no-date workloads are preserved in drafts but marked pending;
- source mode and duration survive edit and draft restoration;
- backend integer-minute calculations are authoritative;
- derived totals always equal their detailed distribution;
- no formatting behavior bypasses or duplicates PR #9's serialized autosave queue;
- regression and full CI verification pass before the PR is considered ready.
