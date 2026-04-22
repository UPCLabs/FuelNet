package com.fuelnet.fuelnet.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppUserMeDto {
    private Long id;
    private String name;
    private String email;
    private String address;
    private LocalDate birthDate;
    private String gender;
}
