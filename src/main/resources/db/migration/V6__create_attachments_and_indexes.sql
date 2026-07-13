CREATE TABLE attachment (
 id UUID PRIMARY KEY,
 owner_type VARCHAR(30) NOT NULL,
 owner_id UUID NOT NULL,
 original_filename VARCHAR(255) NOT NULL,
 stored_path VARCHAR(500) NOT NULL UNIQUE,
 content_type VARCHAR(100) NOT NULL,
 size_bytes BIGINT NOT NULL,
 checksum_sha256 VARCHAR(64) NOT NULL,
 status VARCHAR(20) NOT NULL,
 created_at TIMESTAMP NOT NULL
);
CREATE INDEX ix_attachment_owner ON attachment(owner_type,owner_id);
