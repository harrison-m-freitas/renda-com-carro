package dev.harrison.rendacomcarro.expense.application;

import dev.harrison.rendacomcarro.expense.domain.MonthlyOdometerClosing;
import dev.harrison.rendacomcarro.expense.infrastructure.MonthlyOdometerClosingRepository;
import dev.harrison.rendacomcarro.shared.domain.DomainValidationException;
import dev.harrison.rendacomcarro.vehicle.application.VehicleOdometerService;
import dev.harrison.rendacomcarro.vehicle.application.VehicleService;
import dev.harrison.rendacomcarro.vehicle.domain.OdometerReadingSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonthlyOdometerClosingService {
    private final MonthlyOdometerClosingRepository repository;
    private final VehicleService vehicles;
    private final MonthlyMileageInferenceService inference;
    private final VehicleOdometerService odometerService;

    public MonthlyOdometerClosingService(
        MonthlyOdometerClosingRepository repository,
        VehicleService vehicles,
        MonthlyMileageInferenceService inference,
        VehicleOdometerService odometerService
    ) {
        this.repository = repository;
        this.vehicles = vehicles;
        this.inference = inference;
        this.odometerService = odometerService;
    }

    public record CreateCommand(
        UUID vehicleId,
        YearMonth month,
        BigDecimal initialOdometer,
        BigDecimal finalOdometer,
        BigDecimal professionalKilometers,
        String adjustmentReason
    ) {}

    public record ConfirmCommand(
        UUID vehicleId,
        YearMonth month,
        boolean manualAdjustment,
        BigDecimal confirmedInitialOdometer,
        BigDecimal confirmedFinalOdometer,
        BigDecimal confirmedProfessionalKilometers,
        String adjustmentReason,
        boolean confirmWarnings
    ) {}

    @Transactional(readOnly = true)
    public MonthlyMileagePreview preview(UUID vehicleId, YearMonth month) {
        return inference.infer(vehicleId, month);
    }

    @Transactional
    public MonthlyOdometerClosing confirm(ConfirmCommand command) {
        if (command == null || command.vehicleId() == null || command.month() == null) {
            throw new IllegalArgumentException("Veículo e mês são obrigatórios");
        }

        MonthlyMileagePreview preview = inference.infer(command.vehicleId(), command.month());
        if (preview.hasBlockingAlerts()) {
            throw new DomainValidationException(joinAlerts(preview.blockingAlerts()));
        }
        if (!preview.warnings().isEmpty() && !command.confirmWarnings()) {
            throw new DomainValidationException(
                "Confirme os avisos da prévia antes de salvar o fechamento"
            );
        }

        BigDecimal initial = valueOrInference(
            command.confirmedInitialOdometer(), preview.inferredInitialOdometer());
        BigDecimal end = valueOrInference(
            command.confirmedFinalOdometer(), preview.inferredFinalOdometer());
        BigDecimal professional = valueOrInference(
            command.confirmedProfessionalKilometers(), preview.professionalKilometers());

        var vehicle = vehicles.get(command.vehicleId());
        LocalDateTime confirmedAt = LocalDateTime.now();
        MonthlyOdometerClosing closing = MonthlyOdometerClosing.confirm(
            vehicle,
            command.month(),
            preview.inferredInitialOdometer(),
            preview.inferredFinalOdometer(),
            preview.professionalKilometers(),
            preview.initialOrigin(),
            preview.finalOrigin(),
            preview.calculatedAt(),
            initial,
            end,
            professional,
            command.adjustmentReason(),
            confirmedAt
        );

        MonthlyOdometerClosing saved = repository.save(closing);
        odometerService.registerReading(
            command.vehicleId(),
            saved.getFinalOdometer(),
            closingReadingTime(command.month(), confirmedAt),
            OdometerReadingSource.MONTHLY_CLOSING,
            saved.getId()
        );
        return saved;
    }

    @Transactional
    public MonthlyOdometerClosing create(CreateCommand command) {
        if (command == null || command.vehicleId() == null || command.month() == null) {
            throw new IllegalArgumentException("Veículo e mês são obrigatórios");
        }
        if (repository.findByVehicleIdAndReferenceMonth(
            command.vehicleId(), command.month().atDay(1)).isPresent()) {
            throw new IllegalArgumentException(
                "Já existe fechamento de quilometragem para o veículo neste mês"
            );
        }
        var vehicle = vehicles.get(command.vehicleId());
        return repository.save(MonthlyOdometerClosing.create(
            vehicle,
            command.month(),
            command.initialOdometer(),
            command.finalOdometer(),
            command.professionalKilometers(),
            command.adjustmentReason()
        ));
    }

    @Transactional(readOnly = true)
    public MonthlyOdometerClosing get(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Fechamento não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<MonthlyOdometerClosing> listAll() {
        return repository.findAllByOrderByReferenceMonthDesc();
    }

    private static BigDecimal valueOrInference(BigDecimal confirmed, BigDecimal inferred) {
        if (inferred == null) {
            throw new DomainValidationException("A prévia não possui quilometragem suficiente");
        }
        return confirmed == null ? inferred : confirmed;
    }

    private static String joinAlerts(List<MileageAlert> alerts) {
        return alerts.stream().map(MileageAlert::message).collect(Collectors.joining(" "));
    }

    private static LocalDateTime closingReadingTime(YearMonth month, LocalDateTime now) {
        return YearMonth.from(now).equals(month)
            ? now
            : month.atEndOfMonth().atTime(LocalTime.MAX);
    }
}
