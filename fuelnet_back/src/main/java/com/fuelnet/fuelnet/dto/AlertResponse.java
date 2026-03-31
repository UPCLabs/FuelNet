package com.fuelnet.fuelnet.dto;

import com.fuelnet.fuelnet.enums.FuelType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertResponse {

    private Long id;
    private FuelType fuelType;
    private BigDecimal levelAtAlert;
    private BigDecimal percentageAtAlert;
    private BigDecimal thresholdUsed;
    private LocalDateTime createdAt;
    private boolean read;
}
