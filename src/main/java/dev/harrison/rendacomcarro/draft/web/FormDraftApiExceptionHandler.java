package dev.harrison.rendacomcarro.draft.web;

import dev.harrison.rendacomcarro.draft.application.FormDraftConflictException;
import dev.harrison.rendacomcarro.shared.domain.DomainConflictException;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import dev.harrison.rendacomcarro.shared.domain.ResourceNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = FormDraftController.class)
public class FormDraftApiExceptionHandler {
    @ExceptionHandler({DomainValidationException.class, IllegalArgumentException.class})
    ResponseEntity<Map<String, String>> badRequest(RuntimeException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(FormDraftConflictException.class)
    ResponseEntity<ConflictResponse> versionConflict(FormDraftConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ConflictResponse(
            exception.getMessage(),
            FormDraftResponse.from(exception.getCurrent())
        ));
    }

    @ExceptionHandler(DomainConflictException.class)
    ResponseEntity<Map<String, String>> conflict(DomainConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("message", exception.getMessage()));
    }

    private record ConflictResponse(String message, FormDraftResponse current) {
    }
}
