package com.fuelnet.fuelnet.repositories;

import com.fuelnet.fuelnet.models.FuelAlert;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IFuelAlertRepository extends JpaRepository<FuelAlert, Long> {
    List<FuelAlert> findByTank_StationIdOrderByCreatedAtDesc(Long stationId);
    long countByTank_StationIdAndReadFalse(Long stationId);
}
