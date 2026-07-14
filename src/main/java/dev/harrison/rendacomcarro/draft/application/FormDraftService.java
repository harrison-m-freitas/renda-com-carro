package dev.harrison.rendacomcarro.draft.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.harrison.rendacomcarro.draft.domain.FormDraft;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.draft.infrastructure.FormDraftRepository;
import dev.harrison.rendacomcarro.security.application.CurrentUserService;
import dev.harrison.rendacomcarro.shared.domain.DomainConflictException;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormDraftService {
    public static final int MAX_PAYLOAD_BYTES = 65_536;
    public static final int MAX_CONTEXT_KEY_LENGTH = 160;

    public record SaveDraftCommand(
        FormDraftType formType,
        String contextKey,
        int schemaVersion,
        int currentStep,
        Long expectedVersion,
        ObjectNode payload,
        boolean validateCurrentStep,
        boolean force
    ) {
        public SaveDraftCommand(
            FormDraftType formType,
            String contextKey,
            int schemaVersion,
            int currentStep,
            Long expectedVersion,
            ObjectNode payload,
            boolean force
        ) {
            this(
                formType,
                contextKey,
                schemaVersion,
                currentStep,
                expectedVersion,
                payload,
                false,
                force
            );
        }
    }

    public record DraftView(
        UUID id,
        FormDraftType formType,
        String contextKey,
        int schemaVersion,
        int currentStep,
        ObjectNode payload,
        long version,
        LocalDateTime updatedAt,
        LocalDateTime expiresAt
    ) {
    }

    private final FormDraftRepository repository;
    private final FormDraftDefinitionRegistry definitions;
    private final CurrentUserService currentUser;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final EntityManager entityManager;

    public FormDraftService(
        FormDraftRepository repository,
        FormDraftDefinitionRegistry definitions,
        CurrentUserService currentUser,
        ObjectMapper mapper,
        Clock clock,
        EntityManager entityManager
    ) {
        this.repository = repository;
        this.definitions = definitions;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Optional<DraftView> find(
        String username,
        FormDraftType type,
        String contextKey
    ) {
        FormDraftDefinition definition = definitions.require(type);
        String normalizedKey = normalizeAndCheckKey(definition, contextKey);
        LocalDateTime now = LocalDateTime.now(clock);
        return repository.findByOwnerUsernameAndFormTypeAndContextKey(
                username, type, normalizedKey
            )
            .filter(draft -> draft.getExpiresAt().isAfter(now))
            .map(draft -> toView(draft, definition));
    }

    @Transactional
    public DraftView save(String username, SaveDraftCommand command) {
        if (command == null || command.formType() == null) {
            throw new DomainValidationException("Tipo de rascunho é obrigatório.");
        }
        FormDraftDefinition definition = definitions.require(command.formType());
        if (command.schemaVersion() != definition.schemaVersion()) {
            throw new DomainValidationException("Versão de rascunho incompatível.");
        }
        if (command.currentStep() < 1 || command.currentStep() > definition.maxStep()) {
            throw new DomainValidationException("Etapa de rascunho inválida.");
        }
        if (command.payload() == null) {
            throw new DomainValidationException("O conteúdo do rascunho é obrigatório.");
        }

        String contextKey = normalizeAndCheckKey(definition, command.contextKey());
        ObjectNode normalized = definition.normalizeAndValidate(
            command.payload().deepCopy(),
            command.currentStep(),
            command.validateCurrentStep()
        );
        String json = writeAndCheckSize(normalized);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plusDays(7);

        Optional<FormDraft> existing = repository
            .findByOwnerUsernameAndFormTypeAndContextKey(
                username,
                command.formType(),
                contextKey
            );

        if (existing.isPresent() && !existing.orElseThrow().getExpiresAt().isAfter(now)) {
            repository.delete(existing.orElseThrow());
            repository.flush();
            existing = Optional.empty();
        }

        if (existing.isEmpty()) {
            if (command.expectedVersion() != null) {
                throw new DomainConflictException("O rascunho informado não existe mais.");
            }
            FormDraft created = FormDraft.create(
                currentUser.require(username),
                command.formType(),
                contextKey,
                command.schemaVersion(),
                command.currentStep(),
                json,
                now,
                expiresAt
            );
            return toView(repository.saveAndFlush(created), definition);
        }

        FormDraft draft = existing.orElseThrow();
        if (!command.force()
            && (command.expectedVersion() == null
                || draft.getVersion() != command.expectedVersion())) {
            throw new FormDraftConflictException(toView(draft, definition));
        }

        draft.update(
            command.schemaVersion(),
            command.currentStep(),
            json,
            now,
            expiresAt
        );
        try {
            return toView(repository.saveAndFlush(draft), definition);
        } catch (ObjectOptimisticLockingFailureException exception) {
            entityManager.clear();
            FormDraft current = repository
                .findByOwnerUsernameAndFormTypeAndContextKey(
                    username,
                    command.formType(),
                    contextKey
                )
                .orElseThrow(() -> new DomainConflictException(
                    "O rascunho informado não existe mais."
                ));
            throw new FormDraftConflictException(toView(current, definition));
        }
    }

    @Transactional
    public void discard(
        String username,
        FormDraftType type,
        String contextKey,
        Long expectedVersion
    ) {
        FormDraftDefinition definition = definitions.require(type);
        String normalizedKey = normalizeAndCheckKey(definition, contextKey);
        Optional<FormDraft> existing = repository
            .findByOwnerUsernameAndFormTypeAndContextKey(
                username, type, normalizedKey
            );
        if (existing.isEmpty()) {
            return;
        }
        FormDraft draft = existing.orElseThrow();
        if (expectedVersion != null && draft.getVersion() != expectedVersion) {
            throw new FormDraftConflictException(toView(draft, definition));
        }
        repository.delete(draft);
        repository.flush();
    }

    @Transactional(readOnly = true)
    public List<DraftView> listActive(String username, FormDraftType type) {
        FormDraftDefinition definition = definitions.require(type);
        LocalDateTime now = LocalDateTime.now(clock);
        return repository
            .findAllByOwnerUsernameAndFormTypeAndExpiresAtAfterOrderByUpdatedAtDesc(
                username, type, now
            )
            .stream()
            .map(draft -> toView(draft, definition))
            .toList();
    }

    @Transactional
    public void complete(String username, FormDraftType type, String contextKey) {
        FormDraftDefinition definition = definitions.require(type);
        String normalizedKey = normalizeAndCheckKey(definition, contextKey);
        repository.deleteByOwnerUsernameAndFormTypeAndContextKey(
            username, type, normalizedKey
        );
    }

    @Transactional
    public long deleteExpired() {
        return repository.deleteByExpiresAtLessThanEqual(LocalDateTime.now(clock));
    }

    private String normalizeAndCheckKey(
        FormDraftDefinition definition,
        String contextKey
    ) {
        String normalized = definition.normalizeContextKey(contextKey);
        if (normalized.length() > MAX_CONTEXT_KEY_LENGTH) {
            throw new DomainValidationException("A chave do rascunho é muito longa.");
        }
        return normalized;
    }

    private String writeAndCheckSize(ObjectNode payload) {
        try {
            String json = mapper.writeValueAsString(payload);
            if (json.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
                throw new DomainValidationException(
                    "O rascunho excede o limite de 64 KiB."
                );
            }
            return json;
        } catch (JsonProcessingException exception) {
            throw new DomainValidationException(
                "Não foi possível validar o conteúdo do rascunho."
            );
        }
    }

    private DraftView toView(FormDraft draft, FormDraftDefinition definition) {
        try {
            JsonNode parsed = mapper.readTree(draft.getPayloadJson());
            if (!(parsed instanceof ObjectNode objectNode)) {
                throw new IllegalStateException("Conteúdo de rascunho inválido no banco.");
            }
            ObjectNode migrated = definition.migrate(
                draft.getSchemaVersion(),
                objectNode.deepCopy()
            );
            ObjectNode normalized = definition.normalizeAndValidate(
                migrated,
                draft.getCurrentStep(),
                false
            );
            return new DraftView(
                draft.getId(),
                draft.getFormType(),
                draft.getContextKey(),
                definition.schemaVersion(),
                draft.getCurrentStep(),
                normalized,
                draft.getVersion(),
                draft.getUpdatedAt(),
                draft.getExpiresAt()
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Conteúdo de rascunho inválido no banco.", exception);
        }
    }
}
