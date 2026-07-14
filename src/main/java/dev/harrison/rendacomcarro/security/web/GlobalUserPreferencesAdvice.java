package dev.harrison.rendacomcarro.security.web;

import dev.harrison.rendacomcarro.security.application.UserTimeZoneService;
import java.time.Clock;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalUserPreferencesAdvice {
    private final UserTimeZoneService timeZones;
    private final Clock applicationClock;

    public GlobalUserPreferencesAdvice(UserTimeZoneService timeZones, Clock applicationClock) {
        this.timeZones = timeZones;
        this.applicationClock = applicationClock;
    }

    @ModelAttribute
    void exposeTimeZonePreferences(Model model, Authentication authentication) {
        String defaultZone = applicationClock.getZone().getId();
        String savedZone = null;
        if (authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken)) {
            savedZone = timeZones.savedTimeZoneId(authentication.getName()).orElse(null);
        }
        model.addAttribute("savedTimeZoneId", savedZone);
        model.addAttribute("defaultTimeZoneId", defaultZone);
        model.addAttribute("activeTimeZoneId", savedZone == null ? defaultZone : savedZone);
    }
}
