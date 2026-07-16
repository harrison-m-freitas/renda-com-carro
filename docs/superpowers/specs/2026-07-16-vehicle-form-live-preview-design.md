# Vehicle form unified preview design

## Context

The vehicle form currently uses the shared guided-form structure, but its internal sections still read as separate cards. The goal form already establishes the desired visual direction: one unified shell, a continuous primary column, a secondary result rail, and flush step blocks separated by subtle dividers.

This change will align the vehicle form with that pattern while making the right rail operationally useful. The rail will render a live vehicle preview, including a plate representation that updates as the user types, the vehicle identity, operational parameters, and acquisition data.

## Goals

- Make the vehicle form read as one coherent surface instead of stacked cards.
- Preserve the existing guided mobile flow and all current `data-vehicle-*` hooks.
- Add persisted tank capacity and expected fuel consumption fields.
- Display calculated actual fuel consumption separately from the editable expected value.
- Update the right rail live as the user edits the form.
- Prepare the domain model for future autonomy and fuel-cost calculations.

## Non-goals

- Implement fuel-price tracking in this change.
- Implement route or nearby-fuel-station planning.
- Replace the current guided-form autosave architecture.
- Introduce vehicle categories, engine specifications, or VIN data.
- Treat calculated actual consumption as a manually editable field.

## Visual architecture

### Unified shell

The vehicle form will use a two-column shell matching the goal form's visual hierarchy:

- Main column: page title, progress indicator on mobile, and the form sections.
- Aside column: live vehicle preview and form actions.
- Desktop: `minmax(0, 1fr)` main column plus a fixed-width aside.
- Mobile: a single column with the preview placed after identification and before operational fields; actions remain fixed at the bottom according to the current mobile behavior.

The outer shell owns the border, radius, background, and shadow. Internal guided steps are flush blocks with no independent border, background, radius, or shadow. Adjacent sections are separated by a subtle horizontal rule and top spacing.

### Sections

The form keeps three semantic sections:

1. Identification
   - Make
   - Model
   - Year
   - Plate
   - Display name

2. Operation
   - Fuel type
   - Odometer
   - Tank capacity
   - Expected fuel consumption
   - Actual fuel consumption, read-only

3. Acquisition
   - Purchase price

The current section numbering and mobile step behavior are preserved. Identification remains the first mobile step. Operation and acquisition remain grouped in the second mobile step unless existing JavaScript or acceptance tests require a different grouping.

## Live vehicle preview

### Identity header

The preview shows:

- A two-letter glyph derived from the model, falling back to the make and then `VE`.
- Display name when provided.
- Otherwise, `Make Model` as the primary label.
- A secondary line with `Make Model · Year`.

Empty values use neutral placeholders and must not create visually broken strings or repeated separators.

### Plate preview

The plate representation updates on every relevant input event.

Rules:

- Characters are normalized to uppercase.
- Only characters accepted by the existing plate field behavior are displayed.
- The old Brazilian format may render as `ABC-1234`.
- The Mercosur format renders as `ABC1D23` without an inserted hyphen.
- While incomplete, missing positions use a muted placeholder character instead of shifting the plate layout unpredictably.
- The live preview is decorative and does not replace field validation.
- The actual input remains the authoritative form control.

The preview includes a compact Brazilian/Mercosur-inspired visual treatment without attempting to reproduce a government document exactly.

### Technical summary

The aside displays:

- Fuel type
- Current or initial odometer
- Tank capacity in litres
- Expected consumption in km/L
- Actual consumption in km/L or a neutral insufficient-data state
- Purchase price or a neutral unreported state

Values update live from inputs and selects. Formatting reuses existing localized input utilities where possible instead of duplicating money, odometer, or decimal parsing rules.

### Derived estimates

The preview may show two derived values when enough data exists:

- Expected range = tank capacity × expected consumption
- Actual estimated range = tank capacity × actual consumption

A derived value is hidden or replaced by `Ainda sem dados suficientes` when one of its operands is absent, invalid, or non-positive.

These estimates are advisory and are not persisted in this change.

## Data model

### Persisted fields

Add nullable decimal fields to the vehicle domain:

- `tankCapacityLiters`
- `expectedFuelEfficiencyKmPerLiter`

Recommended semantics:

- Tank capacity: positive decimal, practical upper bound enforced by validation.
- Expected efficiency: positive decimal, practical upper bound enforced by validation.
- Both fields are optional for existing vehicles and during migration.
- New and edit forms expose both fields.

Database column names should follow the repository's existing naming convention. The Flyway migration must be additive and backward compatible.

### Actual consumption

Actual consumption is a calculated operational metric, not a user-authored vehicle attribute.

The form view model exposes it separately as a read-only nullable value, for example `actualFuelEfficiencyKmPerLiter`.

The initial calculation source should use authoritative historical fuel and odometer data already present in the application. If the current data model cannot calculate a reliable value yet, the service returns no value and the UI displays `Ainda sem dados suficientes`.

A reliable future algorithm should prefer full-to-full refuelling intervals or another explicitly defined method that avoids treating partial fills as complete consumption cycles. This design does not invent a misleading number from insufficient history.

## Form and validation behavior

- Existing plate, money, odometer, and text normalization continue to be authoritative.
- Tank capacity and expected efficiency accept localized decimal input.
- Server-side validation is authoritative; client-side constraints improve guidance only.
- Invalid fields participate in the existing error summary and guided-step validation.
- Editing an existing vehicle restores persisted values without generating synthetic autosaves.
- Actual consumption is never submitted as an authoritative editable value.
- Manipulated derived preview values are ignored by the backend.

## JavaScript responsibilities

Extend the existing vehicle form module rather than creating an unrelated controller.

The preview controller will:

- Resolve required controls and preview targets through `data-vehicle-preview-*` hooks.
- Subscribe to `input`, `change`, and custom localized-input events already emitted by shared helpers.
- Format the plate preview deterministically.
- Build identity labels without empty separators.
- Copy display-ready values into the aside.
- Calculate advisory expected and actual ranges.
- Initialize after draft restoration so the first rendered preview matches restored values.
- Avoid triggering saves or modifying the authoritative field values.

The preview code should be implemented as small pure formatting and calculation functions plus a thin DOM-binding layer, making it independently testable.

## Responsive behavior

### Desktop

- Aside is visually continuous with the main shell.
- Aside uses a soft secondary surface and a left divider.
- Preview remains visible while completing the form; sticky positioning may be used only if it does not conflict with the page layout or mobile action behavior.
- Actions remain at the bottom of the aside.

### Mobile

- Shell becomes one column.
- Aside styling loses its left border and desktop-only corner radii.
- Preview becomes a compact block rather than a tall rail.
- Nonessential explanatory copy may be reduced, but all operational values remain accessible.
- Existing fixed action bar and guided next/back behavior remain unchanged.

## Accessibility

- Preview values use semantic labels and values, preferably a definition list.
- Live plate updates are not announced on every keystroke to avoid excessive screen-reader noise.
- A concise summary container may use `aria-live="polite"` only for meaningful calculated-state transitions, such as actual consumption becoming available.
- Read-only calculated consumption is clearly labelled as calculated.
- Insufficient-data states use text, not colour alone.
- Focus order remains within authoritative form controls; decorative preview elements are not focusable.

## Error handling

- Missing preview DOM nodes must not block form submission.
- Invalid or incomplete numeric values produce neutral preview states, not `NaN`, `Infinity`, or misleading zeroes.
- Actual-consumption service failures degrade to no calculated value and must not prevent the vehicle form from loading.
- The backend continues rejecting invalid persisted values regardless of client-side preview behavior.

## Testing strategy

### Domain and persistence

- Flyway migration applies to an existing schema.
- Existing vehicles remain readable with null new fields.
- Tank capacity and expected efficiency round-trip through create and edit flows.
- Server-side validation rejects zero, negative, and values above the defined practical limits.

### Service calculation

- No history returns no actual consumption.
- Insufficient or ambiguous history returns no actual consumption.
- Valid authoritative history returns the expected calculated value.
- Partial-fill scenarios do not produce a falsely precise result unless the chosen algorithm explicitly supports them.

### Controller and view

- Create and edit pages expose the two new editable fields.
- Actual consumption is rendered read-only and is not trusted from request payloads.
- Existing validation errors remain associated with the correct guided step.

### JavaScript

- Plate preview covers old format, Mercosur format, lowercase input, punctuation, incomplete values, paste, selection replacement, and clearing.
- Identity fallback order is name, make/model, and neutral placeholder.
- Expected and actual range calculations handle localized decimals and missing operands.
- Preview initialization runs correctly after draft restoration.
- Preview updates do not emit autosave events beyond those already generated by the authoritative controls.

### Acceptance and regression

- Desktop form renders as one shell with flush sections and an aside.
- Mobile guided navigation, fixed actions, cancellation, and submission remain functional.
- Existing vehicle create/edit tests continue to pass.
- Shared goal and expense form styles remain unaffected because vehicle overrides stay scoped under `.vehicle-form` and `.vehicle-form-page`.

## Implementation boundaries

Likely affected areas:

- Vehicle Flyway migration
- Vehicle entity/domain model
- Vehicle form DTO and validation
- Vehicle application/service mapping
- Vehicle controller/view model
- Vehicle Thymeleaf form
- Vehicle form CSS
- Vehicle form JavaScript
- Java, MockMvc, migration, and JavaScript tests

Unrelated shared-form refactoring is excluded unless a small reusable localized-decimal helper is required by both the new vehicle fields and an existing pattern.

## Acceptance criteria

- The vehicle form presents one unified desktop shell with a continuous main column and an integrated aside.
- Identification, operation, and acquisition no longer appear as independent cards.
- The plate preview updates while typing and supports old and Mercosur formats.
- Tank capacity and expected consumption are persisted and editable.
- Actual consumption is displayed separately as calculated or as insufficient data.
- Expected and actual range estimates appear only when their required values exist.
- Mobile guided behavior and action bar continue to work.
- Existing vehicle functionality and other forms do not regress.
- The work is delivered in a branch and pull request without automatic merge.