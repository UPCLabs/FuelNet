package com.fuelnet.fuelnet.dto;

import java.time.LocalDate;

import com.fuelnet.fuelnet.enums.UserRole;

import lombok.*;

@Data
@Builder
public class StationUserMeDto {
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
