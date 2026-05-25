package com.fuelnet.fuelnet.dto;

import lombok.Data;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
public class CreatePaymentRequest {

    @Email(message = "Email inválido")
    private String userEmail;

    @NotNull(message = "El ID del usuario es obligatorio")
    @Positive(message = "El ID debe ser mayor a 0")
    private Long userId;

    @NotBlank(message = "El tipo de combustible es obligatorio")
    private String fuelType;

    @NotNull(message = "Los galones son obligatorios")
    @DecimalMin(value = "0.01", message = "Los galones deben ser mayores a 0")
    private BigDecimal gallons;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;
}
