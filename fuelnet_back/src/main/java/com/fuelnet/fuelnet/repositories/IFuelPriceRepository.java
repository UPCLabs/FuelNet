package com.fuelnet.fuelnet.repositories;

import com.fuelnet.fuelnet.models.FuelPrice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IFuelPriceRepository extends JpaRepository<FuelPrice, Long> {
    List<FuelPrice> findByStationId(Long stationId);
}
