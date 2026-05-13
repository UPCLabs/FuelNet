package com.fuelnet.fuelnet.services;

import com.fuelnet.fuelnet.dto.FuelPriceDto;
import com.fuelnet.fuelnet.dto.StationCreationRequestDto;
import com.fuelnet.fuelnet.dto.UpdateFuelPriceRequest;
import com.fuelnet.fuelnet.enums.FuelType;
import com.fuelnet.fuelnet.models.FuelPrice;
import com.fuelnet.fuelnet.models.FuelTank;
import com.fuelnet.fuelnet.models.PriceRegulated;
import com.fuelnet.fuelnet.models.Station;
import com.fuelnet.fuelnet.models.StationUser;
import com.fuelnet.fuelnet.repositories.IFuelPriceRepository;
import com.fuelnet.fuelnet.repositories.IPriceRegulatedRepository;
import com.fuelnet.fuelnet.repositories.IStationRepository;
import com.google.firebase.messaging.FirebaseMessagingException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StationService {

    private final IStationRepository stationRepository;
    private final IFuelPriceRepository fuelPriceRepository;
    private final NotificationService notificationService;
    private final IPriceRegulatedRepository priceRegulatedRepository;

    public Optional<Station> getStationById(Long id) {
        return stationRepository.findById(id);
    }

    public List<FuelPrice> getFuelPriceByStation(Long stationId) {
        return fuelPriceRepository.findByStationId(stationId);
    }

    public Station registerStation(StationCreationRequestDto request) {
        PriceRegulated regulated = priceRegulatedRepository
                .findTopByOrderByFetchedAtDesc()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay precios regulados registrados. Intente más tarde."));

        for (FuelPriceDto fuelDto : request.getFuels()) {
            String type = fuelDto.getType().toUpperCase();
            Double submitted = fuelDto.getPrice();

            switch (type) {
                case "CORRIENTE" -> {
                    if (submitted > regulated.getCorriente()) {
                        throw new RuntimeException(
                                "corriente: " + submitted + regulated.getCorriente());
                    }
                }
                case "DIESEL" -> {
                    if (submitted > regulated.getDiesel()) {
                        throw new RuntimeException(
                                "diesel" + submitted + regulated.getDiesel());
                    }
                }
            }
        }

        Station station = Station.builder()
                .name(request.getName())
                .address(request.getAddress())
                .build();

        List<FuelPrice> fuelPrices = request
                .getFuels()
                .stream()
                .map(fuelDto -> FuelPrice.builder()
                        .fuelType(FuelType.valueOf(fuelDto.getType().toUpperCase()))
                        .price(fuelDto.getPrice())
                        .station(station)
                        .build())
                .toList();

        station.setFuelPrices(fuelPrices);

        List<FuelTank> tanks = request.getFuels()
                .stream()
                .map(fuelDto -> FuelTank.builder()
                        .station(station)
                        .fuelType(FuelType.valueOf(fuelDto.getType().toUpperCase()))
                        .capacityGallons(new BigDecimal("5000"))
                        .currentLevelGallons(BigDecimal.ZERO)
                        .build())
                .toList();

        station.setFuelTanks(tanks);

        return stationRepository.save(station);
    }

    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    public void updateFuelPrices(Long stationId, List<UpdateFuelPriceRequest> request, StationUser admin) {

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Estación no encontrada"));

        PriceRegulated regulated = priceRegulatedRepository
                .findTopByOrderByFetchedAtDesc()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay precios regulados registrados. Intente más tarde."));

        for (UpdateFuelPriceRequest r : request) {
            String type = r.getFuelType().toUpperCase();
            Double submitted = r.getPrice();

            switch (type) {
                case "CORRIENTE" -> {
                    if (submitted > regulated.getCorriente()) {
                        throw new RuntimeException(
                                "corriente: " + submitted + regulated.getCorriente());
                    }
                }
                case "DIESEL" -> {
                    if (submitted > regulated.getDiesel()) {
                        throw new RuntimeException(
                                "diesel" + submitted + regulated.getDiesel());
                    }
                }
            }
        }

        List<FuelPrice> prices = fuelPriceRepository.findByStationId(stationId);

        for (UpdateFuelPriceRequest r : request) {

            FuelType type = FuelType.valueOf(r.getFuelType().toUpperCase());

            prices.stream()
                    .filter(p -> p.getFuelType() == type)
                    .findFirst()
                    .ifPresent(p -> p.setPrice(r.getPrice()));
        }

        fuelPriceRepository.saveAll(prices);

        String topic = "station_" + stationId;

        try {
            notificationService.sendToTopic(
                    topic,
                    "⛽ Cambio de precios",
                    "La estación " + station.getName() + " actualizó sus precios");
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }
}
