package com.fuelnet.fuelnet.models;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_movements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tank_id", nullable = false)
    private FuelTank tank;

    @Column(nullable = false)
    private BigDecimal gallonsAdded;

    @Column(nullable = false)
    private BigDecimal levelBefore;

    @Column(nullable = false)
    private BigDecimal levelAfter;

    private String supplier;

    @Column(nullable = false)
    private LocalDateTime rechargeDate;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    @PrePersist
    public void prePersist() {
        if (this.rechargeDate == null) {
            this.rechargeDate = LocalDateTime.now();
        }
    }
}
