package dev.harrison.rendacomcarro.vehicle.domain;

import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "vehicle")
public class Vehicle {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    @Column(name = "vehicle_year", nullable = false)
    private int year;

    @Column(nullable = false, unique = true)
    private String plate;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @Column(name = "primary_vehicle", nullable = false)
    private boolean primaryVehicle;

    @Column(name = "initial_odometer", nullable = false, precision = 12, scale = 1)
    private BigDecimal initialOdometer;

    @Column(name = "current_odometer", nullable = false, precision = 12, scale = 1)
    private BigDecimal currentOdometer;

    @Column(name = "current_odometer_recorded_at", nullable = false)
    private LocalDateTime currentOdometerRecordedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_odometer_source", nullable = false)
    private OdometerReadingSource currentOdometerSource;

    @Column(name = "current_odometer_source_id")
    private UUID currentOdometerSourceId;

    @Column(name = "purchase_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Vehicle() {
    }

    private Vehicle(
        String name,
        String make,
        String model,
        int year,
        String plate,
        FuelType fuelType,
        BigDecimal initialOdometer,
        BigDecimal purchasePrice
    ) {
        BigDecimal normalizedPrice = defaultZero(purchasePrice);
        validateNonNegative(initialOdometer, "Odômetro inicial");
        validateNonNegative(normalizedPrice, "Preço de compra");
        this.id = UUID.randomUUID();
        this.make = requireText(make, "Marca");
        this.model = requireText(model, "Modelo");
        this.name = resolveName(name, this.make, this.model);
        this.year = year;
        this.plate = normalizePlate(plate);
        this.fuelType = requireFuelType(fuelType);
        this.status = VehicleStatus.ACTIVE;
        this.primaryVehicle = false;
        this.initialOdometer = DecimalPolicy.distance(initialOdometer);
        this.currentOdometer = this.initialOdometer;
        this.purchasePrice = DecimalPolicy.money(normalizedPrice);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.currentOdometerRecordedAt = this.createdAt;
        this.currentOdometerSource = OdometerReadingSource.VEHICLE_MANUAL;
        this.currentOdometerSourceId = this.id;
    }

    public static Vehicle create(
        String name,
        String make,
        String model,
        int year,
        String plate,
        FuelType fuelType,
        BigDecimal initialOdometer,
        BigDecimal purchasePrice
    ) {
        return new Vehicle(name, make, model, year, plate, fuelType, initialOdometer, purchasePrice);
    }

    public void updateDetails(
        String name,
        String make,
        String model,
        int year,
        String plate,
        FuelType fuelType,
        BigDecimal purchasePrice
    ) {
        BigDecimal normalizedPrice = defaultZero(purchasePrice);
        validateNonNegative(normalizedPrice, "Preço de compra");
        this.make = requireText(make, "Marca");
        this.model = requireText(model, "Modelo");
        this.name = resolveName(name, this.make, this.model);
        this.year = year;
        this.plate = normalizePlate(plate);
        this.fuelType = requireFuelType(fuelType);
        this.purchasePrice = DecimalPolicy.money(normalizedPrice);
        this.updatedAt = LocalDateTime.now();
    }

    public void applyCurrentOdometer(
        BigDecimal reading,
        LocalDateTime recordedAt,
        OdometerReadingSource source,
        UUID sourceId
    ) {
        validateNonNegative(reading, "Odômetro");
        if (recordedAt == null || source == null) {
            throw new IllegalArgumentException("Data e origem da leitura são obrigatórias");
        }
        this.currentOdometer = DecimalPolicy.distance(reading);
        this.currentOdometerRecordedAt = recordedAt;
        this.currentOdometerSource = source;
        this.currentOdometerSourceId = sourceId;
        this.updatedAt = LocalDateTime.now();
    }

    public void activateAsPrimary() {
        if (status != VehicleStatus.ACTIVE) {
            throw new IllegalStateException("Veículo arquivado não pode ser ativado");
        }
        this.primaryVehicle = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void clearPrimary() {
        this.primaryVehicle = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void archive() {
        this.primaryVehicle = false;
        this.status = VehicleStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }

    private static FuelType requireFuelType(FuelType value) {
        if (value == null) {
            throw new IllegalArgumentException("Combustível é obrigatório");
        }
        return value;
    }

    private static String resolveName(String value, String make, String model) {
        if (value == null || value.isBlank()) {
            return make + " " + model;
        }
        return value.trim();
    }

    private static String normalizePlate(String value) {
        String normalized = requireText(value, "Placa")
            .replaceAll("[\\s-]", "")
            .toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}")) {
            throw new IllegalArgumentException("Informe uma placa no formato ABC-1234 ou ABC1D23");
        }
        return normalized;
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static void validateNonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(field + " não pode ser negativo");
        }
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public String getPlate() { return plate; }
    public FuelType getFuelType() { return fuelType; }
    public VehicleStatus getStatus() { return status; }
    public boolean isPrimaryVehicle() { return primaryVehicle; }
    public BigDecimal getInitialOdometer() { return initialOdometer; }
    public BigDecimal getCurrentOdometer() { return currentOdometer; }
    public LocalDateTime getCurrentOdometerRecordedAt() { return currentOdometerRecordedAt; }
    public OdometerReadingSource getCurrentOdometerSource() { return currentOdometerSource; }
    public UUID getCurrentOdometerSourceId() { return currentOdometerSourceId; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
