package dev.harrison.rendacomcarro.security.application;

import dev.harrison.rendacomcarro.security.domain.AppUser;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserTimeZoneService {
    private final CurrentUserService currentUsers;
    private final Clock applicationClock;

    public UserTimeZoneService(CurrentUserService currentUsers, Clock applicationClock) {
        this.currentUsers = currentUsers;
        this.applicationClock = applicationClock;
    }

    @Transactional(readOnly = true)
    public ZoneId resolve(String username) {
        String saved = requireUser(username).getTimeZoneId();
        return saved == null || saved.isBlank()
            ? applicationClock.getZone()
            : ZoneId.of(saved);
    }

    @Transactional(readOnly = true)
    public LocalDate today(String username) {
        return LocalDate.now(applicationClock.withZone(resolve(username)));
    }

    @Transactional
    public AppUser update(String username, String zoneId) {
        ZoneId validated = validate(zoneId);
        AppUser user = requireUser(username);
        user.updateTimeZone(validated.getId());
        return user;
    }

    @Transactional(readOnly = true)
    public AppUser requireUser(String username) {
        return currentUsers.require(username);
    }

    private ZoneId validate(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            throw new IllegalArgumentException("Fuso horário inválido.");
        }
        try {
            return ZoneId.of(zoneId.trim());
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Fuso horário inválido.");
        }
    }
}
