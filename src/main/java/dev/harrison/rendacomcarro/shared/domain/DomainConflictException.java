package dev.harrison.rendacomcarro.shared.domain;

public class DomainConflictException extends RuntimeException {
    public DomainConflictException(String message) {
        super(message);
    }
}
