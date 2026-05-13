package com.fuelnet.fuelnet.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fuelnet.fuelnet.models.PriceRegulated;

@Repository
public interface IPriceRegulatedRepository extends JpaRepository<PriceRegulated, Long> {

    boolean existsByDocument(String document);

    Optional<PriceRegulated> findTopByOrderByFetchedAtDesc();
}
