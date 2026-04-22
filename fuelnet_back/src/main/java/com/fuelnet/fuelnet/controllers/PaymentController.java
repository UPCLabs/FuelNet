package com.fuelnet.fuelnet.controllers;

import com.fuelnet.fuelnet.dto.*;
import com.fuelnet.fuelnet.models.AppUser;
import com.fuelnet.fuelnet.models.StationUser;
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
    @PreAuthorize("hasAuthority('MANAGE_FACTURATION')")
    public ResponseEntity<PaymentResponse> create(
            @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal StationUser admin) {
        return ResponseEntity.ok(paymentService.createPayment(request, admin));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('MANAGE_FACTURATION')")
    public ResponseEntity<List<PaymentResponse>> getAdminPayments(
            @AuthenticationPrincipal StationUser admin) {
        return ResponseEntity.ok(paymentService.getPaymentsByAdmin(admin));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('MANAGE_FACTURATION')")
    public ResponseEntity<PaymentResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal StationUser admin) {
        return ResponseEntity.ok(paymentService.cancelPayment(id, admin));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal AppUser client) {
        return ResponseEntity.ok(paymentService.getMyPayments(client));
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentSummaryResponse> getSummary(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser client) {
        return ResponseEntity.ok(paymentService.getPaymentSummary(id, client));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> pay(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser client) {
        return ResponseEntity.ok(paymentService.processPayment(id, client));
    }
}
