package dev.harrison.rendacomcarro.draft.application;

public class FormDraftConflictException extends RuntimeException {
    private final FormDraftService.DraftView current;

    public FormDraftConflictException(FormDraftService.DraftView current) {
        super("Este rascunho foi alterado em outro dispositivo.");
        this.current = current;
    }

    public FormDraftService.DraftView getCurrent() {
        return current;
    }
}
