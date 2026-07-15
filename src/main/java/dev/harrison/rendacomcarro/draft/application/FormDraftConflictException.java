package dev.harrison.rendacomcarro.draft.application;

public class FormDraftConflictException extends RuntimeException {
    private final FormDraftService.DraftView current;

    public FormDraftConflictException(FormDraftService.DraftView current) {
        this("Existem alterações diferentes neste rascunho.", current);
    }

    public FormDraftConflictException(
        String message,
        FormDraftService.DraftView current
    ) {
        super(message);
        this.current = current;
    }

    public FormDraftService.DraftView getCurrent() {
        return current;
    }
}
