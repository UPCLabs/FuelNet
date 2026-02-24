package com.fuelnet.fuelnet.dto;

import lombok.*;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String message;
    private String email;
}
