package com.fuelnet.fuelnet.dto;

import com.fuelnet.fuelnet.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private String clientName;
    private String clientEmail;
    private String fuelType;
    private BigDecimal gallons;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
