package com.fuelnet.fuelnet.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {

    private String userEmail;
    private Long userId;

    private String fuelType;

    private BigDecimal gallons;

    private BigDecimal amount;
}
