package com.fuelnet.fuelnet.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.fuelnet.fuelnet.dto.EmployeeResponseDto;
import com.fuelnet.fuelnet.dto.UpdateEmployeeDto;
import com.fuelnet.fuelnet.models.StationUser;
import com.fuelnet.fuelnet.services.EmployeeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasRole('STATION_ADMIN')")
    public ResponseEntity<List<EmployeeResponseDto>> getEmployees(
            @AuthenticationPrincipal StationUser admin) {

        return ResponseEntity.ok(employeeService.getEmployees(admin));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('STATION_ADMIN')")
    public ResponseEntity<EmployeeResponseDto> updateEmployee(
            @PathVariable Long id,
            @RequestBody UpdateEmployeeDto dto,
            @AuthenticationPrincipal StationUser admin) {

        try {
            return ResponseEntity.ok(employeeService.updateEmployee(id, dto, admin));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STATION_ADMIN')")
    public ResponseEntity<?> deleteEmployee(
            @PathVariable Long id,
            @AuthenticationPrincipal StationUser admin) {

        employeeService.deleteEmployee(id, admin);
        return ResponseEntity.ok().build();
    }
}
