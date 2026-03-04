package com.fuelnet.fuelnet.controllers;

import com.fuelnet.fuelnet.dto.FuelPriceDto;
import com.fuelnet.fuelnet.dto.StationCreationRequestDto;
import com.fuelnet.fuelnet.dto.StationPriceResponseDto;
import com.fuelnet.fuelnet.interfaces.IStationService;
import com.fuelnet.fuelnet.models.Station;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/station")
@RequiredArgsConstructor
public class StationController {

    private final IStationService stationService;

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<?> registerStation(
        @RequestBody StationCreationRequestDto request
    ) {
        Station saved = stationService.registerStation(request);

        return ResponseEntity.ok(saved);
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
            .map(fuel ->
                new FuelPriceDto(fuel.getFuelType().name(), fuel.getPrice())
            )
            .toList();

        StationPriceResponseDto response = new StationPriceResponseDto(
            station.getId(),
            station.getName(),
            fuels
        );

        return ResponseEntity.ok(response);
    }
}
