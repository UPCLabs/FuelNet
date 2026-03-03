package com.fuelnet.fuelnet.repositories;

import com.fuelnet.fuelnet.models.FuelPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IFuelPriceRepository extends JpaRepository<FuelPrice, Long> {
    List<FuelPrice> findByStationId(Long stationId);
}
