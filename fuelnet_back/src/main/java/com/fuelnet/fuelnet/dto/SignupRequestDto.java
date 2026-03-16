package com.fuelnet.fuelnet.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequestDto {

    private String name;
    private String email;
    private String password;
}
