# Single Active Vehicle Design

## Goal

Replace the duplicated `status + primaryVehicle` model with a single source of truth: exactly one vehicle may be `ACTIVE`, while other retained vehicles are `INACTIVE` or `ARCHIVED`. Monthly goals are associated with one immutable vehicle instead of a selectable set.

## Vehicle lifecycle

`VehicleStatus` has three values:

- `ACTIVE`: the only vehicle currently used by operational flows.
- `INACTIVE`: retained in history and eligible for reactivation.
- `ARCHIVED`: permanently closed and not eligible for reactivation.

Creating a vehicle always makes it `ACTIVE`. In the same transaction, the current active vehicle becomes `INACTIVE`. Reactivating an `INACTIVE` vehicle follows the same swap. An `ACTIVE` vehicle cannot be archived; another vehicle must be activated or created first. An `INACTIVE` vehicle may be archived. An `ARCHIVED` vehicle cannot be reactivated.

The `primary_vehicle` column and primary-vehicle methods are removed. PostgreSQL enforces at most one active row with a partial unique index, while service transactions lock operational vehicle rows before changing state.

## Monthly goals

Each `MonthlyGoal` has one non-null `Vehicle` through `monthly_goal.vehicle_id`.

- New goals automatically capture the current `ACTIVE` vehicle.
- Existing goals keep their original vehicle when edited, even if that vehicle later becomes `INACTIVE` or `ARCHIVED`.
- The goal form does not submit or allow choosing a vehicle.
- The form renders the associated vehicle as read-only context.
- A new goal cannot be created when no active vehicle exists.
- Operational suggestions for a new goal use the active vehicle; suggestions while editing use the vehicle already stored on that goal.

The existing `monthly_goal_vehicle` join table is migrated to the singular foreign key and then removed. If historical data contains multiple associations, migration selection is deterministic: active association first, then oldest vehicle creation timestamp, then UUID.

## Financial suggestion boundary

The public goal suggestion endpoint no longer accepts arbitrary vehicle IDs. It accepts `month` and optional `goalId`:

- no `goalId`: resolve the current active vehicle;
- with `goalId`: resolve that goal's stored vehicle.

The internal expense, odometer and obligation queries use one vehicle ID. Shared professional expenses with `vehicle_id IS NULL` continue to enter once.

## Drafts and interface

The goal draft schema removes `vehicleIds`; legacy drafts may contain the field, but normalization discards it. The multi-vehicle picker module, chips, checkbox markup and picker-specific tests are removed. Generic checkbox-array draft support remains because it is framework behavior and not vehicle-domain behavior.

Vehicle list and detail pages display `Ativo`, `Inativo` or `Arquivado`. Only inactive vehicles expose `Ativar` and `Arquivar` actions. Active vehicles display that they are currently in operation and cannot be archived directly.

## Migration strategy

Because `V13` and `V14` may already have executed in development environments, they are not rewritten. A new `V15` migration:

1. converts extra active vehicles to `INACTIVE`, preferring the existing active primary vehicle and otherwise the oldest active vehicle;
2. drops `ux_vehicle_single_primary`;
3. drops `vehicle.primary_vehicle`;
4. creates `ux_vehicle_single_active`;
5. adds and backfills `monthly_goal.vehicle_id` from `monthly_goal_vehicle`;
6. makes the foreign key non-null;
7. creates an index for vehicle goal history;
8. drops `monthly_goal_vehicle`.

## Error handling

- Creating a goal without an active vehicle returns a form-level message directing the user to register or activate a vehicle.
- Activating an archived vehicle raises `Veículo arquivado não pode ser ativado.`
- Archiving the active vehicle raises `Ative outro veículo antes de arquivar o veículo atual.`
- Database uniqueness remains the final concurrency guard.

## Verification

Tests cover lifecycle swaps, archived restrictions, unique active enforcement, singular goal persistence, edit preservation, suggestion resolution, draft normalization, rendered UI, Flyway migration and the complete existing CI including ARM64 smoke, backup and restore.
