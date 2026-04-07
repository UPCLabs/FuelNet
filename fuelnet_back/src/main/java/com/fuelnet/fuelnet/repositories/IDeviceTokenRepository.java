package com.fuelnet.fuelnet.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fuelnet.fuelnet.models.DeviceToken;

public interface IDeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUserId(Long userId);
}
