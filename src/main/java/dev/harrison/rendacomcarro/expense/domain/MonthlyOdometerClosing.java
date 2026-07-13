package dev.harrison.rendacomcarro.expense.domain;

import dev.harrison.rendacomcarro.vehicle.domain.Vehicle;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name="monthly_odometer_closing")
public class MonthlyOdometerClosing {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id") private Vehicle vehicle;
 @Column(name="reference_month",nullable=false) private LocalDate referenceMonth;
 @Column(name="initial_odometer",nullable=false,precision=12,scale=1) private BigDecimal initialOdometer;
 @Column(name="final_odometer",nullable=false,precision=12,scale=1) private BigDecimal finalOdometer;
 @Column(name="professional_kilometers",nullable=false,precision=12,scale=1) private BigDecimal professionalKilometers;
 @Column(name="personal_kilometers",nullable=false,precision=12,scale=1) private BigDecimal personalKilometers;
 @Column(name="professional_percentage",nullable=false,precision=5,scale=4) private BigDecimal professionalPercentage;
 @Column(name="adjustment_reason") private String adjustmentReason;
 protected MonthlyOdometerClosing(){}
 public UUID getId(){return id;}
}
