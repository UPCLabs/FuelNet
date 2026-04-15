package com.fuelnet.fuelnet.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SignupRequestDto {

    private String name;
    private String email;
    private String username;
    private String password;
    private String address;
    private String birthday;
    private String role;
    private String gender;

}
