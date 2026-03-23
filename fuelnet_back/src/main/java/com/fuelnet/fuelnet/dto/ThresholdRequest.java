package com.fuelnet.fuelnet.dto;

import com.fuelnet.fuelnet.enums.FuelType;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ThresholdRequest {

    private FuelType fuelType;

    private BigDecimal thresholdPercentage;
}
