package com.fuelnet.fuelnet.repositories;

import com.fuelnet.fuelnet.models.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IInventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findByTankIdOrderByRechargeDateDesc(Long tankId);

    List<InventoryMovement> findByTank_StationIdOrderByRechargeDateDesc(Long stationId);
}
