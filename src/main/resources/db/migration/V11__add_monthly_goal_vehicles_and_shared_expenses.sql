CREATE TABLE monthly_goal_vehicle (
    monthly_goal_id UUID NOT NULL REFERENCES monthly_goal(id) ON DELETE CASCADE,
    vehicle_id UUID NOT NULL REFERENCES vehicle(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (monthly_goal_id, vehicle_id)
);

CREATE INDEX ix_monthly_goal_vehicle_vehicle
    ON monthly_goal_vehicle(vehicle_id, monthly_goal_id);

INSERT INTO monthly_goal_vehicle(monthly_goal_id, vehicle_id)
SELECT goal.id, selected_vehicle.id
FROM monthly_goal goal
CROSS JOIN LATERAL (
    SELECT vehicle.id
    FROM vehicle
    WHERE vehicle.status = 'ACTIVE'
    ORDER BY vehicle.primary_vehicle DESC, vehicle.created_at ASC
    LIMIT 1
) selected_vehicle;

ALTER TABLE expense ALTER COLUMN vehicle_id DROP NOT NULL;
DROP INDEX IF EXISTS ix_expense_vehicle_date;
CREATE INDEX ix_expense_vehicle_date ON expense(vehicle_id, expense_date);
