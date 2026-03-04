package com.fuelnet.fuelnet.services;

import com.fuelnet.fuelnet.dto.StationCreationRequestDto;
import com.fuelnet.fuelnet.enums.FuelType;
import com.fuelnet.fuelnet.interfaces.IStationService;
import com.fuelnet.fuelnet.models.FuelPrice;
import com.fuelnet.fuelnet.models.Station;
import com.fuelnet.fuelnet.repositories.IFuelPriceRepository;
import com.fuelnet.fuelnet.repositories.IStationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    @Override
    public Station registerStation(StationCreationRequestDto request) {
        Station station = Station.builder()
            .name(request.getName())
            .address(request.getAddress())
            .build();

        List<FuelPrice> fuelPrices = request
            .getFuels()
            .stream()
            .map(fuelDto ->
                FuelPrice.builder()
                    .fuelType(FuelType.valueOf(fuelDto.getType()))
                    .price(fuelDto.getPrice())
                    .station(station)
                    .build()
            )
            .toList();

        station.setFuelPrices(fuelPrices);

        return stationRepository.save(station);
    }
}
