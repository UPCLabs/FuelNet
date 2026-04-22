package com.fuelnet.fuelnet.services;

import com.fuelnet.fuelnet.dto.*;
import com.fuelnet.fuelnet.models.*;
import com.fuelnet.fuelnet.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final IFuelTankRepository tankRepository;
    private final IInventoryMovementRepository movementRepository;

    public List<FuelTankResponse> getDashboard(StationUser admin) {
        Long stationId = getStationId(admin);
        return tankRepository.findByStationId(stationId)
                .stream().map(this::toTankResponse).collect(Collectors.toList());
    }

    @Transactional
    public InventoryMovementResponse recharge(RechargeRequest request, StationUser admin) {
        Long stationId = getStationId(admin);

        FuelTank tank = tankRepository.findByStationIdAndFuelType(stationId, request.getFuelType())
                .orElseThrow(() -> new RuntimeException(
                        "No existe tanque de " + request.getFuelType() + " en esta estación"));

        BigDecimal levelBefore = tank.getCurrentLevelGallons();
        BigDecimal levelAfter = levelBefore.add(request.getGallonsAdded());

        if (levelAfter.compareTo(tank.getCapacityGallons()) > 0) {
            throw new RuntimeException(
                    "La recarga excede la capacidad del tanque. Disponible: "
                            + tank.getCapacityGallons().subtract(levelBefore) + " galones");
        }

        tank.setCurrentLevelGallons(levelAfter);
        tankRepository.save(tank);

        InventoryMovement movement = InventoryMovement.builder()
                .tank(tank)
                .gallonsAdded(request.getGallonsAdded())
                .levelBefore(levelBefore)
                .levelAfter(levelAfter)
                .supplier(request.getSupplier())
                .rechargeDate(request.getRechargeDate())
                .notes(request.getNotes())
                .registeredBy(admin)
                .build();

        return toMovementResponse(movementRepository.save(movement));
    }

    public List<InventoryMovementResponse> getHistory(StationUser admin) {
        Long stationId = getStationId(admin);
        return movementRepository.findByTank_StationIdOrderByRechargeDateDesc(stationId)
                .stream().map(this::toMovementResponse).collect(Collectors.toList());
    }

    private Long getStationId(StationUser admin) {
        if (admin.getStation() == null) {
            throw new RuntimeException("Este administrador no tiene una estación asignada");
        }
        return admin.getStation().getId();
    }

    private FuelTankResponse toTankResponse(FuelTank t) {
        return FuelTankResponse.builder()
                .id(t.getId())
                .fuelType(t.getFuelType())
                .capacityGallons(t.getCapacityGallons())
                .currentLevelGallons(t.getCurrentLevelGallons())
                .fillPercentage(t.getFillPercentage())
                .lastUpdated(t.getLastUpdated())
                .build();
    }

    private InventoryMovementResponse toMovementResponse(InventoryMovement m) {
        return InventoryMovementResponse.builder()
                .id(m.getId())
                .fuelType(m.getTank().getFuelType())
                .gallonsAdded(m.getGallonsAdded())
                .levelBefore(m.getLevelBefore())
                .levelAfter(m.getLevelAfter())
                .fillPercentageAfter(m.getTank().getFillPercentage())
                .supplier(m.getSupplier())
                .rechargeDate(m.getRechargeDate())
                .notes(m.getNotes())
                .registeredBy(m.getRegisteredBy().getName())
                .build();
    }
}
