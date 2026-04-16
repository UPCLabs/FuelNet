package com.fuelnet.fuelnet.dto;

import lombok.Data;

@Data
public class AdminRegisterRequest {

    private Long pendingUserId;
    private Boolean accepted;
    private String roleRequested;
}
