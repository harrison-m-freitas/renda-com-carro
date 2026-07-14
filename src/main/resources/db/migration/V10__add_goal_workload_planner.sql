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

ALTER TABLE planned_work_day
    ADD COLUMN allocated_duration_minutes BIGINT;

WITH ranked_days AS (
    SELECT
        pwd.id,
        mg.calculated_month_minutes,
        COUNT(*) OVER (PARTITION BY pwd.monthly_goal_id) AS day_count,
        ROW_NUMBER() OVER (
            PARTITION BY pwd.monthly_goal_id
            ORDER BY pwd.work_date, pwd.id
        ) AS day_number
    FROM planned_work_day pwd
    JOIN monthly_goal mg ON mg.id = pwd.monthly_goal_id
), allocations AS (
    SELECT
        id,
        calculated_month_minutes / day_count
            + CASE
                WHEN day_number <= calculated_month_minutes % day_count THEN 1
                ELSE 0
              END AS allocated_minutes
    FROM ranked_days
)
UPDATE planned_work_day pwd
SET allocated_duration_minutes = allocations.allocated_minutes,
    planned_hours = ROUND(allocations.allocated_minutes::numeric / 60, 2)
FROM allocations
WHERE allocations.id = pwd.id;

ALTER TABLE planned_work_day
    ALTER COLUMN allocated_duration_minutes SET NOT NULL,
    ADD CONSTRAINT ck_planned_work_day_allocated_duration_minutes
        CHECK (allocated_duration_minutes >= 0);
