package com.fuelnet.fuelnet.controllers;

import com.fuelnet.fuelnet.dto.*;
import com.fuelnet.fuelnet.models.StationUser;
import com.fuelnet.fuelnet.services.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STATION_ADMIN')")
    public ResponseEntity<List<FuelTankResponse>> getDashboard(
            @AuthenticationPrincipal StationUser admin) {
        return ResponseEntity.ok(inventoryService.getDashboard(admin));
    }

    @PostMapping("/recharge")
    @PreAuthorize("hasRole('STATION_ADMIN')")
    public ResponseEntity<InventoryMovementResponse> recharge(
            @RequestBody RechargeRequest request,
            @AuthenticationPrincipal StationUser admin) {
        return ResponseEntity.ok(inventoryService.recharge(request, admin));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('STATION_ADMIN')")
    public ResponseEntity<List<InventoryMovementResponse>> getHistory(
            @AuthenticationPrincipal StationUser admin) {
        return ResponseEntity.ok(inventoryService.getHistory(admin));
    }
}
