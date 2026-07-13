package dev.harrison.rendacomcarro.operation.domain;

import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import dev.harrison.rendacomcarro.shared.domain.DomainConflictException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "operation_shift")
public class Shift {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operational_day_id")
    private OperationalDay operationalDay;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ShiftStatus status;
    @Column(name = "started_at", nullable = false) private LocalDateTime startedAt;
    @Column(name = "ended_at") private LocalDateTime endedAt;
    @Column(name = "initial_odometer", nullable = false, precision = 12, scale = 1) private BigDecimal initialOdometer;
    @Column(name = "final_odometer", precision = 12, scale = 1) private BigDecimal finalOdometer;
    @Column(name = "start_region", nullable = false) private String startRegion;
    @Column(name = "end_region") private String endRegion;
    @Column(columnDefinition = "text") private String notes;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "shift_platform",
        joinColumns = @JoinColumn(name = "shift_id"),
        inverseJoinColumns = @JoinColumn(name = "platform_id")
    )
    private Set<Platform> platforms = new LinkedHashSet<>();
    @ElementCollection
    @CollectionTable(name = "shift_neighborhood", joinColumns = @JoinColumn(name = "shift_id"))
    @Column(name = "neighborhood")
    private Set<String> neighborhoods = new LinkedHashSet<>();

    protected Shift() {}

    private Shift(
        OperationalDay day,
        LocalDateTime start,
        BigDecimal odometer,
        String region,
        Set<Platform> platforms
    ) {
        if (day == null || start == null || odometer == null || odometer.signum() < 0
            || region == null || region.isBlank()) {
            throw new IllegalArgumentException("Dados de abertura do turno são inválidos");
        }
        if (platforms == null || platforms.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos uma plataforma");
        }
        id = UUID.randomUUID();
        operationalDay = day;
        startedAt = start.truncatedTo(ChronoUnit.MICROS);
        initialOdometer = DecimalPolicy.distance(odometer);
        startRegion = region.trim();
        this.platforms.addAll(platforms);
        status = ShiftStatus.OPEN;
    }

    public static Shift open(
        OperationalDay day,
        LocalDateTime start,
        BigDecimal odometer,
        String region,
        Set<Platform> platforms
    ) {
        return new Shift(day, start, odometer, region, platforms);
    }

    public void close(
        LocalDateTime end,
        BigDecimal odometer,
        String region,
        Set<String> served
    ) {
        if (status != ShiftStatus.OPEN) {
            throw new DomainConflictException("Turno não está aberto");
        }
        if (end == null) {
            throw new IllegalArgumentException("Horário final é obrigatório");
        }
        LocalDateTime normalizedEnd = end.truncatedTo(ChronoUnit.MICROS);
        if (normalizedEnd.isBefore(startedAt)) {
            throw new IllegalArgumentException("Horário final deve ser posterior ao inicial");
        }
        if (odometer == null || odometer.compareTo(initialOdometer) < 0) {
            throw new IllegalArgumentException("Odômetro final não pode ser menor que o inicial");
        }
        endedAt = normalizedEnd;
        finalOdometer = DecimalPolicy.distance(odometer);
        endRegion = region == null ? null : region.trim();
        neighborhoods.clear();
        if (served != null) {
            served.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .forEach(neighborhoods::add);
        }
        status = ShiftStatus.CLOSED;
    }

    public BigDecimal getDistance() {
        return finalOdometer == null
            ? BigDecimal.ZERO.setScale(1)
            : finalOdometer.subtract(initialOdometer);
    }

    public Duration getDuration() {
        LocalDateTime effectiveEnd = endedAt == null
            ? LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
            : endedAt;
        return Duration.between(startedAt, effectiveEnd);
    }

    public UUID getId() { return id; }
    public OperationalDay getOperationalDay() { return operationalDay; }
    public ShiftStatus getStatus() { return status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public BigDecimal getInitialOdometer() { return initialOdometer; }
    public BigDecimal getFinalOdometer() { return finalOdometer; }
    public String getStartRegion() { return startRegion; }
    public String getEndRegion() { return endRegion; }
    public Set<Platform> getPlatforms() { return Collections.unmodifiableSet(platforms); }
    public Set<String> getNeighborhoods() { return Collections.unmodifiableSet(neighborhoods); }
}
