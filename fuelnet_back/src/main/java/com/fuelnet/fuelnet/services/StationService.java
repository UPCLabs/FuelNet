package com.fuelnet.fuelnet.services;

import com.fuelnet.fuelnet.models.FuelPrice;
import com.fuelnet.fuelnet.models.Station;
import com.fuelnet.fuelnet.interfaces.IStationService;
import com.fuelnet.fuelnet.repositories.IFuelPriceRepository;
import com.fuelnet.fuelnet.repositories.IStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StationService implements IStationService {

    private final IStationRepository stationRepository;
    private final IFuelPriceRepository fuelPriceRepository;

    public Optional<Station> getStationById(Long id) {
        return stationRepository.findById(id);
    }

    @Override
    public List<FuelPrice> getFuelPriceByStation(Long stationId) {
        return fuelPriceRepository.findByStationId(stationId);
    }
}
