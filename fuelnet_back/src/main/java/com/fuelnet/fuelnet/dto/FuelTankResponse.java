package com.fuelnet.fuelnet.dto;

import com.fuelnet.fuelnet.enums.FuelType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FuelTankResponse {
    private Long id;
    private FuelType fuelType;
    private BigDecimal capacityGallons;
    private BigDecimal currentLevelGallons;
    private BigDecimal fillPercentage;
    private LocalDateTime lastUpdated;
}
