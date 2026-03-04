package com.fuelnet.fuelnet.interfaces;

import com.fuelnet.fuelnet.dto.StationCreationRequestDto;
import com.fuelnet.fuelnet.models.FuelPrice;
import com.fuelnet.fuelnet.models.Station;
import java.util.List;
import java.util.Optional;

public interface IStationService {
    Optional<Station> getStationById(Long id);

    List<FuelPrice> getFuelPriceByStation(Long stationId);

    Station registerStation(StationCreationRequestDto request);
}
