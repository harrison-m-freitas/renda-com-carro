package dev.harrison.rendacomcarro.security.web;

import dev.harrison.rendacomcarro.security.application.UserTimeZoneService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.security.Principal;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-preferences/time-zone")
public class UserTimeZoneController {
    private final UserTimeZoneService timeZones;

    public UserTimeZoneController(UserTimeZoneService timeZones) {
        this.timeZones = timeZones;
    }

    @GetMapping
    public UserTimeZoneResponse show(Principal principal) {
        ZoneId active = timeZones.resolve(principal.getName());
        String saved = timeZones.requireUser(principal.getName()).getTimeZoneId();
        return new UserTimeZoneResponse(saved, active.getId());
    }

    @PutMapping
    public UserTimeZoneResponse update(
        Principal principal,
        @Valid @RequestBody UpdateUserTimeZoneRequest request
    ) {
        var user = timeZones.update(principal.getName(), request.timeZoneId());
        return new UserTimeZoneResponse(user.getTimeZoneId(), user.getTimeZoneId());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> invalidTimeZone(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    public record UpdateUserTimeZoneRequest(@NotBlank String timeZoneId) {
    }

    public record UserTimeZoneResponse(String savedTimeZoneId, String activeTimeZoneId) {
    }
}
