WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY owner_id
               ORDER BY updated_at DESC, id DESC
           ) AS position
    FROM form_draft
    WHERE form_type = 'OBLIGATION'
)
DELETE FROM form_draft
WHERE id IN (
    SELECT id
    FROM ranked
    WHERE position > 1
);

CREATE UNIQUE INDEX ux_form_draft_single_obligation_owner
    ON form_draft (owner_id)
    WHERE form_type = 'OBLIGATION';
