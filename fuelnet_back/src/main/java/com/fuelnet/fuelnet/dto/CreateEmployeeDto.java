package com.fuelnet.fuelnet.dto;

import java.util.List;

import com.fuelnet.fuelnet.enums.Permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEmployeeDto {
    private String name;
    private String email;
    private String password;
    private List<Permission> permissions;
}
