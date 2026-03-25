package com.fuelnet.fuelnet.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(
    name = "tank_thresholds",
    uniqueConstraints = @UniqueConstraint(columnNames = "tank_id")
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TankThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tank_id", nullable = false)
    private FuelTank tank;

    @Column(nullable = false)
    private BigDecimal thresholdPercentage;
}
