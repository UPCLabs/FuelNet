package com.fuelnet.fuelnet.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SignupRequestDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100)
    private String name;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "El username es obligatorio")
    @Size(min = 4, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Solo letras, números y _")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank(message = "La dirección es obligatoria")
    private String address;

    @Past(message = "La fecha debe ser anterior a hoy")
    private LocalDate birthday;

    @NotBlank(message = "Rol obligatorio")
    private String role;

    @NotBlank(message = "Género obligatorio")
    private String gender;
}
