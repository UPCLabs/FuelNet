package com.fuelnet.fuelnet.controllers;

import com.fuelnet.fuelnet.dto.FuelPriceDto;
import com.fuelnet.fuelnet.dto.StationCreationRequestDto;
import com.fuelnet.fuelnet.dto.StationPriceResponseDto;
import com.fuelnet.fuelnet.dto.StationsResponseDto;
import com.fuelnet.fuelnet.dto.UpdateFuelPriceRequest;
import com.fuelnet.fuelnet.models.FuelPrice;
import com.fuelnet.fuelnet.models.Station;
import com.fuelnet.fuelnet.models.User;
import com.fuelnet.fuelnet.repositories.IUserRepository;
import com.fuelnet.fuelnet.services.StationService;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/station")
@RequiredArgsConstructor
public class StationController {

    private final IUserRepository userRepository;
    private final StationService stationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'STATION_ADMIN')")
    public ResponseEntity<?> registerStation(
            @RequestBody StationCreationRequestDto request,
            @AuthenticationPrincipal User user) {
        Station saved = stationService.registerStation(request);

        user.setStation(saved);
        userRepository.save(user);

        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/get-stations")
    public ResponseEntity<?> getStations() {
        List<Station> stations = stationService.getAllStations();

        List<StationsResponseDto> stationDtos = stations
                .stream()
                .map(station -> new StationsResponseDto(
                        station.getId(),
                        station.getName(),
                        station.getAddress()))
                .toList();

        return ResponseEntity.ok(stationDtos);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{id}/prices")
    public ResponseEntity<?> checkPrices(@PathVariable Long id) {
        var optionalStation = stationService.getStationById(id);

        if (optionalStation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Station station = optionalStation.get();

        List<FuelPriceDto> fuels = station
                .getFuelPrices()
                .stream()
                .map(fuel -> new FuelPriceDto(fuel.getFuelType().name(), fuel.getPrice()))
                .toList();

        StationPriceResponseDto response = new StationPriceResponseDto(
                station.getId(),
                station.getName(),
                fuels);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/prices")
    @PreAuthorize("hasAnyRole('STATION_ADMIN')")
    public List<FuelPrice> getMyPrices(
            @AuthenticationPrincipal User user) {
        return stationService.getFuelPriceByStation(user.getStation().getId());
    }

    @PutMapping("/prices")
    @PreAuthorize("hasAnyRole('STATION_ADMIN')")
    public Map<String, String> updatePrices(
            @RequestBody List<UpdateFuelPriceRequest> request,
            @AuthenticationPrincipal User user) {
        stationService.updateFuelPrices(user.getStation().getId(), request, user);
        return Map.of("message", "Precios actualizados");
    }
}
