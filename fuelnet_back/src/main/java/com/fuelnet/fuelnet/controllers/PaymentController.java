package com.fuelnet.fuelnet.controllers;

import com.fuelnet.fuelnet.dto.*;
import com.fuelnet.fuelnet.models.User;
import com.fuelnet.fuelnet.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('STATION_ADMIN')")
    public ResponseEntity<PaymentResponse> create(
            @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(paymentService.createPayment(request, admin));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('STATION_ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getAdminPayments(
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(paymentService.getPaymentsByAdmin(admin));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STATION_ADMIN')")
    public ResponseEntity<PaymentResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(paymentService.cancelPayment(id, admin));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal User client) {
        return ResponseEntity.ok(paymentService.getMyPayments(client));
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentSummaryResponse> getSummary(
            @PathVariable Long id,
            @AuthenticationPrincipal User client) {
        return ResponseEntity.ok(paymentService.getPaymentSummary(id, client));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentResponse> pay(
            @PathVariable Long id,
            @AuthenticationPrincipal User client) {
        return ResponseEntity.ok(paymentService.processPayment(id, client));
    }
}
