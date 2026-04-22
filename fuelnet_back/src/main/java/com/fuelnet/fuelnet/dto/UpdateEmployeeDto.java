package com.fuelnet.fuelnet.dto;

import java.util.List;

import com.fuelnet.fuelnet.enums.Permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeeDto {
    private String name;
    private List<Permission> permissions;
}
