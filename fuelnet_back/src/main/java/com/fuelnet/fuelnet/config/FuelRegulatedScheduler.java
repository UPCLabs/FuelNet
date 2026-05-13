package com.fuelnet.fuelnet.config;

import java.time.LocalDateTime;
import java.util.logging.Logger;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fuelnet.fuelnet.dto.FuelRegulatedDto;
import com.fuelnet.fuelnet.models.PriceRegulated;
import com.fuelnet.fuelnet.repositories.IPriceRegulatedRepository;
import com.fuelnet.fuelnet.services.FuelPriceScraperService;
import com.fuelnet.fuelnet.services.NotificationService;
import com.google.firebase.messaging.FirebaseMessagingException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FuelRegulatedScheduler {

    private final FuelPriceScraperService scraperService;
    private final NotificationService notificationService;
    private final IPriceRegulatedRepository repository;
    private final Logger logger = Logger.getLogger("scheduler");

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        checkAndUpdate();
    }

    @Scheduled(cron = "0 0 6 * * MON")
    public void scheduledCheck() {
        checkAndUpdate();
    }

    private void checkAndUpdate() {
        try {
            FuelRegulatedDto dto = scraperService.runScraper();

            if (repository.existsByDocument(dto.getCircular())) {
                logger.info("Circular {} ya registrada, sin cambios." + dto.getCircular());
                return;
            }

            PriceRegulated entity = new PriceRegulated();
            entity.setDocument(dto.getCircular());
            entity.setUrl(dto.getUrl());
            entity.setCorriente(dto.getPrecios().getCorriente());
            entity.setDiesel(dto.getPrecios().getDiesel());
            entity.setFetchedAt(LocalDateTime.now());

            repository.save(entity);
            logger.info("Nuevos precios guardados: " + dto.getCircular());

            try {
                notificationService.sendPriceUpdate(dto.getCircular(), dto.getUrl());
            } catch (FirebaseMessagingException e) {
                logger.warning("Error enviando notificación de nueva circular: " + e.getMessage());
            }

        } catch (Exception e) {
            logger.severe("Error al obtener precios de combustible" + e.getMessage());
        }
    }
}
