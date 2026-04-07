package com.fuelnet.fuelnet.services;

import com.fuelnet.fuelnet.dto.*;
import com.fuelnet.fuelnet.enums.FuelType;
import com.fuelnet.fuelnet.enums.PaymentStatus;
import com.fuelnet.fuelnet.models.FuelAlert;
import com.fuelnet.fuelnet.models.FuelTank;
import com.fuelnet.fuelnet.models.InventoryMovement;
import com.fuelnet.fuelnet.models.Payment;
import com.fuelnet.fuelnet.models.TankThreshold;
import com.fuelnet.fuelnet.models.User;
import com.fuelnet.fuelnet.repositories.IFuelAlertRepository;
import com.fuelnet.fuelnet.repositories.IFuelTankRepository;
import com.fuelnet.fuelnet.repositories.IInventoryMovementRepository;
import com.fuelnet.fuelnet.repositories.IPaymentRepository;
import com.fuelnet.fuelnet.repositories.ITankThresholdRepository;
import com.fuelnet.fuelnet.repositories.IUserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final IPaymentRepository paymentRepository;
    private final IFuelTankRepository tankRepository;
    private final IInventoryMovementRepository inventoryMovementRepository;
    private final IUserRepository userRepository;
    private final IFuelAlertRepository alertRepository;
    private final ITankThresholdRepository thresholdRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    public PaymentResponse createPayment(
            CreatePaymentRequest request,
            User admin) {
        User client;
        if (request.getUserEmail() != null) {
            client = userRepository
                    .findByEmail(request.getUserEmail())
                    .orElseThrow(() -> new RuntimeException(
                            "Usuario no encontrado con email: " +
                                    request.getUserEmail()));
        } else if (request.getUserId() != null) {
            client = userRepository
                    .findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException(
                            "Usuario no encontrado con ID: " + request.getUserId()));
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

        FuelTank tank = tankRepository
                .findByStationIdAndFuelType(
                        payment.getCreatedBy().getStation().getId(),
                        FuelType.valueOf(payment.getFuelType().toUpperCase()))
                .orElseThrow(() -> new RuntimeException("Tanque no encontrado"));

        if (tank.getCurrentLevelGallons().compareTo(payment.getGallons()) < 0) {
            throw new RuntimeException(
                    "Inventario insuficiente para completar la factura");
        }

        tank.setCurrentLevelGallons(
                tank.getCurrentLevelGallons().subtract(payment.getGallons()));
        tankRepository.save(tank);
        checkAndGenerateAlert(tank, admin);

        Payment saved = paymentRepository.save(payment);

        InventoryMovement movement = InventoryMovement.builder()
                .tank(tank)
                .gallonsAdded(saved.getGallons().negate())
                .levelBefore(tank.getCurrentLevelGallons().add(saved.getGallons()))
                .levelAfter(tank.getCurrentLevelGallons())
                .supplier("Venta")
                .notes("Pago #" + saved.getId())
                .registeredBy(saved.getUser())
                .build();
        inventoryMovementRepository.save(movement);

        notificationService.sendToUser(
                client.getId(),
                "💸 Nueva factura creada",
                "Tienes un nuevo pago pendiente por $" + saved.getAmount());

        return toResponse(saved);
    }

    public List<PaymentResponse> getPaymentsByAdmin(User admin) {
        return paymentRepository
                .findByCreatedById(admin.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getMyPayments(User client) {
        return paymentRepository
                .findByUserId(client.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PaymentSummaryResponse getPaymentSummary(
            Long paymentId,
            User client) {
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
            throw new RuntimeException(
                    "Este pago fue cancelado y no puede procesarse");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        notificationService.sendToUser(
                client.getId(),
                "✅ Pago realizado",
                "Tu pago #" + saved.getId() + " fue procesado correctamente");

        notificationService.sendToUser(
                saved.getCreatedBy().getId(),
                "📥 Pago recibido",
                "El cliente " + client.getName() + " pagó $" + saved.getAmount());

        return toResponse(saved);
    }

    public PaymentResponse cancelPayment(Long paymentId, User admin) {
        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        if (!payment.getCreatedBy().getId().equals(admin.getId())) {
            throw new RuntimeException(
                    "No tienes permiso para cancelar este pago");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new RuntimeException(
                    "No se puede cancelar un pago ya realizado");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        Payment saved = paymentRepository.save(payment);
        notificationService.sendToUser(
                saved.getUser().getId(),
                "❌ Pago cancelado",
                "Tu pago #" + saved.getId() + " fue cancelado");
        return toResponse(saved);
    }

    private Payment getPaymentForClient(Long paymentId, User client) {
        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        if (!payment.getUser().getId().equals(client.getId())) {
            throw new RuntimeException("No tienes acceso a este pago");
        }
        return payment;
    }

    private void checkAndGenerateAlert(FuelTank tank, User admin) {
        BigDecimal threshold = thresholdRepository
                .findByTankId(tank.getId())
                .map(TankThreshold::getThresholdPercentage)
                .orElse(new BigDecimal("15"));

        BigDecimal currentPercentage = tank.getFillPercentage();

        if (currentPercentage.compareTo(threshold) <= 0) {
            boolean yaExiste = alertRepository
                    .findByTank_StationIdOrderByCreatedAtDesc(
                            tank.getStation().getId())
                    .stream()
                    .anyMatch(
                            a -> a.getTank().getId().equals(tank.getId()) && !a.isRead());

            if (!yaExiste) {
                FuelAlert alert = FuelAlert.builder()
                        .tank(tank)
                        .levelAtAlert(tank.getCurrentLevelGallons())
                        .percentageAtAlert(currentPercentage)
                        .thresholdUsed(threshold)
                        .build();
                alertRepository.save(alert);

                if (admin.getStation() != null) {
                    emailService.sendLowFuelAlert(admin.getEmail(), alert);
                }
            }
        }
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
