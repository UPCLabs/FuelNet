package com.fuelnet.fuelnet.dto;

import lombok.Data;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Data
public class UpdateFuelPriceRequest {

    @NotNull(message = "El tipo de combustible es obligatorio")
    private String fuelType;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal price;
}
