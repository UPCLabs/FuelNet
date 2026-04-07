package com.fuelnet.fuelnet.dto;

import lombok.Data;

@Data
public class UpdateFuelPriceRequest {
    private String fuelType;
    private Double price;
}
