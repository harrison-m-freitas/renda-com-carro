package dev.harrison.rendacomcarro.draft.domain;

import dev.harrison.rendacomcarro.security.domain.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "form_draft")
public class FormDraft {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "form_type", nullable = false, length = 40)
    private FormDraftType formType;

    @Column(name = "context_key", nullable = false, length = 160)
    private String contextKey;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "current_step", nullable = false)
    private int currentStep;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private String payloadJson;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected FormDraft() {
    }

    public static FormDraft create(
        AppUser owner,
        FormDraftType type,
        String contextKey,
        int schemaVersion,
        int currentStep,
        String payloadJson,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
    ) {
        FormDraft draft = new FormDraft();
        draft.id = UUID.randomUUID();
        draft.owner = Objects.requireNonNull(owner, "owner");
        draft.formType = Objects.requireNonNull(type, "type");
        draft.contextKey = Objects.requireNonNull(contextKey, "contextKey");
        draft.schemaVersion = schemaVersion;
        draft.currentStep = currentStep;
        draft.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson");
        draft.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        draft.updatedAt = createdAt;
        draft.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        return draft;
    }

    public void update(
        int schemaVersion,
        int currentStep,
        String payloadJson,
        LocalDateTime updatedAt,
        LocalDateTime expiresAt
    ) {
        this.schemaVersion = schemaVersion;
        this.currentStep = currentStep;
        this.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public UUID getId() {
        return id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public FormDraftType getFormType() {
        return formType;
    }

    public String getContextKey() {
        return contextKey;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
