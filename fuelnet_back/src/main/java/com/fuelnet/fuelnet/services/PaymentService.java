package com.fuelnet.fuelnet.services;

import com.fuelnet.fuelnet.dto.*;
import com.fuelnet.fuelnet.enums.PaymentStatus;
import com.fuelnet.fuelnet.models.Payment;
import com.fuelnet.fuelnet.models.User;
import com.fuelnet.fuelnet.repositories.IPaymentRepository;
import com.fuelnet.fuelnet.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final IPaymentRepository paymentRepository;
    private final IUserRepository userRepository;

    public PaymentResponse createPayment(CreatePaymentRequest request, User admin) {

        User client;
        if (request.getUserEmail() != null) {
            client = userRepository.findByEmail(request.getUserEmail())
                    .orElseThrow(
                            () -> new RuntimeException("Usuario no encontrado con email: " + request.getUserEmail()));
        } else if (request.getUserId() != null) {
            client = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + request.getUserId()));
        } else {
            throw new RuntimeException("Debe proveer userEmail o userId");
        }

        Payment payment = Payment.builder()
                .user(client)
                .createdBy(admin)
                .fuelType(request.getFuelType())
                .gallons(request.getGallons())
                .amount(request.getAmount())
                .status(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);
        return toResponse(saved);
    }

    public List<PaymentResponse> getPaymentsByAdmin(User admin) {
        return paymentRepository.findByCreatedById(admin.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<PaymentResponse> getMyPayments(User client) {
        return paymentRepository.findByUserId(client.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PaymentSummaryResponse getPaymentSummary(Long paymentId, User client) {
        Payment payment = getPaymentForClient(paymentId, client);

        return PaymentSummaryResponse.builder()
                .paymentId(payment.getId())
                .fuelType(payment.getFuelType())
                .gallons(payment.getGallons())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .message("Revisa el resumen antes de confirmar tu pago")
                .build();
    }

    public PaymentResponse processPayment(Long paymentId, User client) {
        Payment payment = getPaymentForClient(paymentId, client);

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Este pago ya fue procesado");
        }
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new RuntimeException("Este pago fue cancelado y no puede procesarse");
        }

        // Simulación: marcamos como pagado directamente
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        return toResponse(saved);
    }

    public PaymentResponse cancelPayment(Long paymentId, User admin) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        if (!payment.getCreatedBy().getId().equals(admin.getId())) {
            throw new RuntimeException("No tienes permiso para cancelar este pago");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("No se puede cancelar un pago ya realizado");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        return toResponse(paymentRepository.save(payment));
    }

    private Payment getPaymentForClient(Long paymentId, User client) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        if (!payment.getUser().getId().equals(client.getId())) {
            throw new RuntimeException("No tienes acceso a este pago");
        }
        return payment;
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .clientName(p.getUser().getName())
                .clientEmail(p.getUser().getEmail())
                .fuelType(p.getFuelType())
                .gallons(p.getGallons())
                .amount(p.getAmount())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt())
                .build();
    }
}
