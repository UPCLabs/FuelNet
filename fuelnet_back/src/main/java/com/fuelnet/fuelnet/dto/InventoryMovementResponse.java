package com.fuelnet.fuelnet.dto;

import com.fuelnet.fuelnet.enums.FuelType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InventoryMovementResponse {
    private Long id;
    private FuelType fuelType;
    private BigDecimal gallonsAdded;
    private BigDecimal levelBefore;
    private BigDecimal levelAfter;
    private BigDecimal fillPercentageAfter;
    private String supplier;
    private LocalDateTime rechargeDate;
    private String notes;
    private String registeredBy;
}
