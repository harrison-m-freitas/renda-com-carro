CREATE TABLE form_draft (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES app_user(id),
    form_type VARCHAR(40) NOT NULL,
    context_key VARCHAR(160) NOT NULL,
    schema_version INTEGER NOT NULL CHECK (schema_version > 0),
    current_step INTEGER NOT NULL CHECK (current_step > 0),
    payload_json JSONB NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_form_draft_owner_type_context
        UNIQUE (owner_id, form_type, context_key)
);

CREATE INDEX idx_form_draft_owner_type_updated
    ON form_draft(owner_id, form_type, updated_at DESC);

CREATE INDEX idx_form_draft_expires_at
    ON form_draft(expires_at);
