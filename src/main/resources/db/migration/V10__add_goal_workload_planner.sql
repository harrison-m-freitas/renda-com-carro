ALTER TABLE monthly_goal
    ADD COLUMN workload_periodicity VARCHAR(16),
    ADD COLUMN entered_duration_minutes BIGINT,
    ADD COLUMN calculated_month_minutes BIGINT;

UPDATE monthly_goal
SET workload_periodicity = 'MONTHLY',
    entered_duration_minutes = CAST(ROUND(planned_hours * 60) AS BIGINT),
    calculated_month_minutes = CAST(ROUND(planned_hours * 60) AS BIGINT);

ALTER TABLE monthly_goal
    ALTER COLUMN workload_periodicity SET NOT NULL,
    ALTER COLUMN entered_duration_minutes SET NOT NULL,
    ALTER COLUMN calculated_month_minutes SET NOT NULL;

ALTER TABLE monthly_goal
    ADD CONSTRAINT ck_monthly_goal_workload_periodicity
        CHECK (workload_periodicity IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    ADD CONSTRAINT ck_monthly_goal_entered_duration_minutes
        CHECK (entered_duration_minutes >= 0),
    ADD CONSTRAINT ck_monthly_goal_calculated_month_minutes
        CHECK (calculated_month_minutes >= 0);
