ALTER TABLE vehicle
  ADD COLUMN current_odometer_recorded_at TIMESTAMP,
  ADD COLUMN current_odometer_source VARCHAR(40),
  ADD COLUMN current_odometer_source_id UUID;

UPDATE vehicle
SET current_odometer_recorded_at = created_at,
    current_odometer_source = 'VEHICLE_MANUAL',
    current_odometer_source_id = id
WHERE current_odometer_recorded_at IS NULL;

ALTER TABLE vehicle
  ALTER COLUMN current_odometer_recorded_at SET NOT NULL,
  ALTER COLUMN current_odometer_source SET NOT NULL;

ALTER TABLE monthly_odometer_closing
  ADD COLUMN manual_adjustment BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN inferred_initial_odometer NUMERIC(12,1),
  ADD COLUMN inferred_final_odometer NUMERIC(12,1),
  ADD COLUMN inferred_professional_kilometers NUMERIC(12,1),
  ADD COLUMN initial_odometer_origin VARCHAR(50),
  ADD COLUMN final_odometer_origin VARCHAR(50),
  ADD COLUMN calculated_at TIMESTAMP,
  ADD COLUMN confirmed_at TIMESTAMP;

UPDATE monthly_odometer_closing
SET calculated_at = CURRENT_TIMESTAMP,
    confirmed_at = CURRENT_TIMESTAMP
WHERE confirmed_at IS NULL;
