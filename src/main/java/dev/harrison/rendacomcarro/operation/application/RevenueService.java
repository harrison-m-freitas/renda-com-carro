package dev.harrison.rendacomcarro.operation.application;

import dev.harrison.rendacomcarro.operation.domain.DataSource;
import dev.harrison.rendacomcarro.operation.domain.Platform;
import dev.harrison.rendacomcarro.operation.domain.Revenue;
import dev.harrison.rendacomcarro.operation.domain.RevenueType;
import dev.harrison.rendacomcarro.operation.domain.Shift;
import dev.harrison.rendacomcarro.operation.infrastructure.PlatformRepository;
import dev.harrison.rendacomcarro.operation.infrastructure.RevenueRepository;
import dev.harrison.rendacomcarro.operation.infrastructure.ShiftRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevenueService {
    private final RevenueRepository revenueRepository;
    private final ShiftRepository shiftRepository;
    private final PlatformRepository platformRepository;

    public RevenueService(
        RevenueRepository revenueRepository,
        ShiftRepository shiftRepository,
        PlatformRepository platformRepository
    ) {
        this.revenueRepository = revenueRepository;
        this.shiftRepository = shiftRepository;
        this.platformRepository = platformRepository;
    }

    public record CreateRevenueCommand(
        UUID shiftId,
        UUID tripId,
        UUID platformId,
        RevenueType type,
        LocalDate competenceDate,
        LocalDate receivedDate,
        BigDecimal grossAmount,
        BigDecimal platformFee,
        BigDecimal netAmount,
        BigDecimal tipAmount,
        BigDecimal bonusAmount,
        DataSource source,
        String externalReference
    ) {}

    @Transactional
    public Revenue create(CreateRevenueCommand command) {
        Shift shift = shiftRepository.findById(command.shiftId())
            .orElseThrow(() -> new IllegalArgumentException("Turno não encontrado"));
        Platform platform = platformRepository.findById(command.platformId())
            .orElseThrow(() -> new IllegalArgumentException("Plataforma não encontrada"));

        if (command.externalReference() != null
            && !command.externalReference().isBlank()
            && revenueRepository.existsByPlatformIdAndExternalReference(
                platform.getId(), command.externalReference().trim())) {
            throw new IllegalArgumentException("Referência externa duplicada para a plataforma");
        }

        Revenue revenue = Revenue.create(
            shift,
            platform,
            command.type(),
            command.competenceDate(),
            command.receivedDate(),
            command.grossAmount(),
            command.platformFee(),
            command.netAmount(),
            command.tipAmount(),
            command.bonusAmount(),
            command.source(),
            command.externalReference()
        );
        return revenueRepository.save(revenue);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumByCompetence(LocalDate date) {
        return revenueRepository.sumNetByCompetenceDate(date)
            .orElse(BigDecimal.ZERO)
            .setScale(2);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumByReceivedDate(LocalDate date) {
        return revenueRepository.sumNetByReceivedDate(date)
            .orElse(BigDecimal.ZERO)
            .setScale(2);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumByCompetence(LocalDate start, LocalDate end) {
        validateRange(start, end);
        return revenueRepository.sumNetByCompetenceDateBetween(start, end)
            .orElse(BigDecimal.ZERO)
            .setScale(2);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumByReceivedDate(LocalDate start, LocalDate end) {
        validateRange(start, end);
        return revenueRepository.sumNetByReceivedDateBetween(start, end)
            .orElse(BigDecimal.ZERO)
            .setScale(2);
    }

    @Transactional(readOnly = true)
    public List<Revenue> listByShift(UUID shiftId) {
        return revenueRepository.findAllByShiftIdOrderByCompetenceDateDesc(shiftId);
    }

    private static void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("Período de receita inválido");
        }
    }
}
