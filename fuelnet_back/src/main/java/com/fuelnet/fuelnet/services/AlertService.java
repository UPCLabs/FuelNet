package com.fuelnet.fuelnet.services;

import com.fuelnet.fuelnet.dto.AlertResponse;
import com.fuelnet.fuelnet.dto.ThresholdRequest;
import com.fuelnet.fuelnet.models.*;
import com.fuelnet.fuelnet.repositories.*;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final IFuelAlertRepository alertRepository;
    private final ITankThresholdRepository thresholdRepository;
    private final IFuelTankRepository tankRepository;

    public List<AlertResponse> getAlerts(StationUser admin) {
        Long stationId = admin.getStation().getId();
        return alertRepository
                .findByTank_StationIdOrderByCreatedAtDesc(stationId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(StationUser admin) {
        return alertRepository.countByTank_StationIdAndReadFalse(
                admin.getStation().getId());
    }

    public void markAsRead(Long alertId, StationUser admin) {
        FuelAlert alert = alertRepository
                .findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));

        if (!alert
                .getTank()
                .getStation()
                .getId()
                .equals(admin.getStation().getId())) {
            throw new RuntimeException("No tienes acceso a esta alerta");
        }
        alert.setRead(true);
        alertRepository.save(alert);
    }

    public void updateThreshold(ThresholdRequest request, StationUser admin) {
        FuelTank tank = tankRepository
                .findByStationIdAndFuelType(
                        admin.getStation().getId(),
                        request.getFuelType())
                .orElseThrow(() -> new RuntimeException("Tanque no encontrado"));

        TankThreshold threshold = thresholdRepository
                .findByTankId(tank.getId())
                .orElse(TankThreshold.builder().tank(tank).build());

        threshold.setThresholdPercentage(request.getThresholdPercentage());
        thresholdRepository.save(threshold);
    }

    private AlertResponse toResponse(FuelAlert a) {
        return AlertResponse.builder()
                .id(a.getId())
                .fuelType(a.getTank().getFuelType())
                .levelAtAlert(a.getLevelAtAlert())
                .percentageAtAlert(a.getPercentageAtAlert())
                .thresholdUsed(a.getThresholdUsed())
                .createdAt(a.getCreatedAt())
                .read(a.isRead())
                .build();
    }
}
