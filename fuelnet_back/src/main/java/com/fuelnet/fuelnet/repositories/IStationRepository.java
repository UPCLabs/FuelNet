package com.fuelnet.fuelnet.repositories;

import com.fuelnet.fuelnet.models.Station;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IStationRepository extends JpaRepository<Station, Long> {
}
