package dev.harrison.rendacomcarro.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.harrison.rendacomcarro.security.application.CurrentUserService;
import dev.harrison.rendacomcarro.security.application.UserTimeZoneService;
import dev.harrison.rendacomcarro.security.domain.AppUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserTimeZoneServiceTest {
    private CurrentUserService currentUsers;
    private AppUser user;
    private UserTimeZoneService service;

    @BeforeEach
    void setUp() {
        currentUsers = mock(CurrentUserService.class);
        user = new AppUser("owner", "encoded-password");
        when(currentUsers.require("owner")).thenReturn(user);
        Clock clock = Clock.fixed(Instant.parse("2026-07-14T10:30:00Z"), ZoneOffset.UTC);
        service = new UserTimeZoneService(currentUsers, clock);
    }

    @Test
    void todayUsesSavedZoneInsteadOfServerZone() {
        user.updateTimeZone("Pacific/Kiritimati");

        assertThat(service.today("owner")).isEqualTo(LocalDate.of(2026, 7, 15));
    }

    @Test
    void applicationZoneIsUsedWhenUserHasNoPreference() {
        assertThat(service.resolve("owner")).isEqualTo(ZoneOffset.UTC);
        assertThat(service.today("owner")).isEqualTo(LocalDate.of(2026, 7, 14));
    }

    @Test
    void validZoneIsCanonicalizedAndSaved() {
        AppUser updated = service.update("owner", "America/Sao_Paulo");

        assertThat(updated).isSameAs(user);
        assertThat(user.getTimeZoneId()).isEqualTo("America/Sao_Paulo");
    }

    @Test
    void invalidZoneIsRejectedWithoutChangingPreference() {
        assertThatThrownBy(() -> service.update("owner", "Mars/Olympus"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Fuso horário inválido.");

        assertThat(user.getTimeZoneId()).isNull();
    }
}
