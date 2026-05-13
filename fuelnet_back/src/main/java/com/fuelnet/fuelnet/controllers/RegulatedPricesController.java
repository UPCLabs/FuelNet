package com.fuelnet.fuelnet.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fuelnet.fuelnet.models.PriceRegulated;
import com.fuelnet.fuelnet.repositories.IPriceRegulatedRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/regulated-prices")
@RequiredArgsConstructor
public class RegulatedPricesController {

    private final IPriceRegulatedRepository repository;

    @GetMapping("/current")
    public ResponseEntity<PriceRegulated> getCurrentPrices() {
        return repository.findTopByOrderByFetchedAtDesc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
