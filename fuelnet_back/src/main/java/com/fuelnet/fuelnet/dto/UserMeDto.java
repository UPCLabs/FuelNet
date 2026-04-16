package com.fuelnet.fuelnet.dto;

import com.fuelnet.fuelnet.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserMeDto {

    private Long id;
    private String name;
    private String email;
    private String username;
    private String address;
    private LocalDate birthDate;
    private String gender;
    private UserRole role;
    private Long stationId;
}
