package com.fuelnet.fuelnet.repositories;

import com.fuelnet.fuelnet.enums.FuelType;
import com.fuelnet.fuelnet.models.FuelTank;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IFuelTankRepository extends JpaRepository<FuelTank, Long> {
    List<FuelTank> findByStationId(Long stationId);

    Optional<FuelTank> findByStationIdAndFuelType(Long stationId, FuelType fuelType);
}
