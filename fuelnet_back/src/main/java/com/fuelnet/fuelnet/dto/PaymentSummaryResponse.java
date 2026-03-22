package com.fuelnet.fuelnet.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PaymentSummaryResponse {
    private Long paymentId;
    private String fuelType;
    private BigDecimal gallons;
    private BigDecimal amount;
    private String status;
    private String message;
}
