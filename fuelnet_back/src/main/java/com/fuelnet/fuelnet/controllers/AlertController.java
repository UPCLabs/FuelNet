package com.fuelnet.fuelnet.controllers;

import com.fuelnet.fuelnet.dto.*;
import com.fuelnet.fuelnet.models.User;
import com.fuelnet.fuelnet.services.AlertService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @PreAuthorize("hasRole('STATION_ADMIN')")
    public ResponseEntity<List<AlertResponse>> getAlerts(
        @AuthenticationPrincipal User admin
    ) {
        return ResponseEntity.ok(alertService.getAlerts(admin));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('STATION_ADMIN')")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
        @AuthenticationPrincipal User admin
    ) {
        return ResponseEntity.ok(
            Map.of("count", alertService.getUnreadCount(admin))
        );
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('STATION_ADMIN')")
    public ResponseEntity<Void> markAsRead(
        @PathVariable Long id,
        @AuthenticationPrincipal User admin
    ) {
        alertService.markAsRead(id, admin);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/threshold")
    @PreAuthorize("hasRole('STATION_ADMIN')")
    public ResponseEntity<Void> updateThreshold(
        @RequestBody ThresholdRequest request,
        @AuthenticationPrincipal User admin
    ) {
        alertService.updateThreshold(request, admin);
        return ResponseEntity.ok().build();
    }
}
