package com.fuelnet.fuelnet.repositories;

import com.fuelnet.fuelnet.models.TankThreshold;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ITankThresholdRepository
    extends JpaRepository<TankThreshold, Long>
{
    Optional<TankThreshold> findByTankId(Long tankId);
}
