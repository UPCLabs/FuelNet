package com.fuelnet.fuelnet.dto;

import com.fuelnet.fuelnet.enums.FuelType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

@Data
public class RechargeRequest {

    @NotNull(message = "El tipo de combustible es obligatorio")
    private FuelType fuelType;

    @NotNull(message = "Los galones agregados son obligatorios")
    @DecimalMin(value = "0.01", message = "Debe agregar más de 0 galones")
    private BigDecimal gallonsAdded;

    @NotBlank(message = "El proveedor es obligatorio")
    @Size(max = 100)
    private String supplier;

    @NotNull(message = "La fecha es obligatoria")
    @PastOrPresent(message = "La fecha no puede ser futura")
    private LocalDateTime rechargeDate;

    @Size(max = 500)
    private String notes;
}
