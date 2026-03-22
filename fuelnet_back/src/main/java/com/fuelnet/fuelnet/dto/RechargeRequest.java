package com.fuelnet.fuelnet.dto;

import com.fuelnet.fuelnet.enums.FuelType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RechargeRequest {

    private FuelType fuelType;

    private BigDecimal gallonsAdded;

    private String supplier;

    private LocalDateTime rechargeDate;
    private String notes;
}
