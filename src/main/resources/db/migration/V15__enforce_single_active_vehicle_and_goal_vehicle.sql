WITH ranked_active AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            ORDER BY primary_vehicle DESC, created_at ASC, id ASC
        ) AS position
    FROM vehicle
    WHERE status = 'ACTIVE'
)
UPDATE vehicle AS target
SET
    status = 'INACTIVE',
    updated_at = CURRENT_TIMESTAMP
FROM ranked_active AS ranked
WHERE target.id = ranked.id
  AND ranked.position > 1;

DROP INDEX IF EXISTS ux_vehicle_single_primary;
ALTER TABLE vehicle DROP COLUMN primary_vehicle;

ALTER TABLE vehicle
    ADD CONSTRAINT ck_vehicle_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'));

CREATE UNIQUE INDEX ux_vehicle_single_active
    ON vehicle ((status))
    WHERE status = 'ACTIVE';

ALTER TABLE monthly_goal ADD COLUMN vehicle_id UUID;

UPDATE monthly_goal AS goal
SET vehicle_id = selected.vehicle_id
FROM (
    SELECT DISTINCT ON (link.monthly_goal_id)
        link.monthly_goal_id,
        link.vehicle_id
    FROM monthly_goal_vehicle AS link
    JOIN vehicle ON vehicle.id = link.vehicle_id
    ORDER BY
        link.monthly_goal_id,
        CASE vehicle.status
            WHEN 'ACTIVE' THEN 0
            WHEN 'INACTIVE' THEN 1
            ELSE 2
        END,
        vehicle.created_at ASC,
        vehicle.id ASC
) AS selected
WHERE goal.id = selected.monthly_goal_id;

UPDATE monthly_goal AS goal
SET vehicle_id = (
    SELECT vehicle.id
    FROM vehicle
    ORDER BY
        CASE vehicle.status
            WHEN 'ACTIVE' THEN 0
            WHEN 'INACTIVE' THEN 1
            ELSE 2
        END,
        vehicle.created_at ASC,
        vehicle.id ASC
    LIMIT 1
)
WHERE goal.vehicle_id IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM monthly_goal WHERE vehicle_id IS NULL) THEN
        RAISE EXCEPTION 'Não foi possível associar todas as metas mensais a um veículo.';
    END IF;
END $$;

ALTER TABLE monthly_goal
    ALTER COLUMN vehicle_id SET NOT NULL,
    ADD CONSTRAINT fk_monthly_goal_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicle(id);

CREATE INDEX ix_monthly_goal_vehicle_history
    ON monthly_goal(vehicle_id, reference_month DESC);

DROP TABLE monthly_goal_vehicle;
