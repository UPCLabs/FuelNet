package com.fuelnet.fuelnet.repositories;

import com.fuelnet.fuelnet.models.StationUser;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IStationUserRepository extends JpaRepository<StationUser, Long> {
    boolean existsByEmail(String email);

    Optional<StationUser> findByEmail(String email);

    List<StationUser> findByStationId(Long stationId);

    Optional<StationUser> findByIdAndStationId(Long id, Long stationId);
}
