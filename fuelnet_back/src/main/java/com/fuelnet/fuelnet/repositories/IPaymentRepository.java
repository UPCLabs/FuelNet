package com.fuelnet.fuelnet.repositories;

import com.fuelnet.fuelnet.models.Payment;
import com.fuelnet.fuelnet.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IPaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserId(Long userId);

    List<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status);

    List<Payment> findByCreatedById(Long adminId);
}
