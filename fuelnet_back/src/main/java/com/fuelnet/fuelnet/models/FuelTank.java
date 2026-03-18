package com.fuelnet.fuelnet.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fuelnet.fuelnet.enums.FuelType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fuel_tanks", uniqueConstraints = @UniqueConstraint(columnNames = { "station_id", "fuel_type" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuelTank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    @JsonIgnore
    private Station station;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType;

    @Column(nullable = false)
    private BigDecimal capacityGallons;

    @Column(nullable = false)
    private BigDecimal currentLevelGallons;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    public BigDecimal getFillPercentage() {
        if (capacityGallons == null || capacityGallons.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentLevelGallons
                .divide(capacityGallons, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
