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
        validateNonNegative(initialOdometer, "Odômetro inicial");
        validateNonNegative(purchasePrice, "Preço de compra");
        this.id = UUID.randomUUID();
        this.name = requireText(name, "Nome");
        this.make = requireText(make, "Marca");
        this.model = requireText(model, "Modelo");
        this.year = year;
        this.plate = requireText(plate, "Placa").toUpperCase(Locale.ROOT);
        this.fuelType = fuelType;
        this.status = VehicleStatus.ACTIVE;
        this.primaryVehicle = false;
        this.initialOdometer = DecimalPolicy.distance(initialOdometer);
        this.currentOdometer = this.initialOdometer;
        this.purchasePrice = DecimalPolicy.money(purchasePrice);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
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

    public void update(String name, String make, String model, int year, String plate, FuelType fuelType,
                       BigDecimal currentOdometer, BigDecimal purchasePrice) {
        validateNonNegative(currentOdometer, "Odômetro atual");
        validateNonNegative(purchasePrice, "Preço de compra");
        this.name = requireText(name, "Nome");
        this.make = requireText(make, "Marca");
        this.model = requireText(model, "Modelo");
        this.year = year;
        this.plate = requireText(plate, "Placa").toUpperCase(Locale.ROOT);
        this.fuelType = fuelType;
        this.currentOdometer = DecimalPolicy.distance(currentOdometer);
        this.purchasePrice = DecimalPolicy.money(purchasePrice);
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
    public BigDecimal getPurchasePrice() { return purchasePrice; }
}
