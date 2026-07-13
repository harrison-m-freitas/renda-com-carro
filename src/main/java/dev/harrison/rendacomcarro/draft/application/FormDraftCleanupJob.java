package dev.harrison.rendacomcarro.draft.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FormDraftCleanupJob {
    private final FormDraftService drafts;

    public FormDraftCleanupJob(FormDraftService drafts) {
        this.drafts = drafts;
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "${app.timezone}")
    public void removeExpiredDrafts() {
        drafts.deleteExpired();
    }
}
