package com.fuelnet.fuelnet.dto;

import lombok.*;

@Data
@Builder
public class PendingUserDto {

    private Long id;
    private String name;
    private String email;
    private String address;
    private String birthDate;
    private String gender;
    private String roleRequested;

}
