package com.fuelnet.fuelnet.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "fuel_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuelAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tank_id", nullable = false)
    private FuelTank tank;

    @Column(nullable = false)
    private BigDecimal levelAtAlert;

    @Column(nullable = false)
    private BigDecimal percentageAtAlert;

    @Column(nullable = false)
    private BigDecimal thresholdUsed;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean read;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }
}
