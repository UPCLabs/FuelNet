package com.fuelnet.fuelnet.controllers;

import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.fuelnet.fuelnet.dto.RegisterNotificationTokenDto;
import com.fuelnet.fuelnet.models.AppUser;
import com.fuelnet.fuelnet.models.DeviceToken;
import com.fuelnet.fuelnet.models.StationUser;
import com.fuelnet.fuelnet.repositories.IDeviceTokenRepository;

import lombok.*;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final IDeviceTokenRepository tokenRepository;

    @PostMapping("/register")
    public String registerToken(@RequestBody RegisterNotificationTokenDto request,
            @AuthenticationPrincipal Object user) {

        Long userId = -1L;

        if (user instanceof StationUser stationUser) {
            userId = stationUser.getId();
        } else if (user instanceof AppUser appUser) {
            userId = appUser.getId();
        }

        if (userId < 0) {
            throw new RuntimeException("NO ID??");
        }

        Optional<DeviceToken> existing = tokenRepository.findByToken(request.getToken());

        if (existing.isPresent()) {
            DeviceToken token = existing.get();
            token.setUserId(userId);
            tokenRepository.save(token);
        } else {
            DeviceToken token = DeviceToken.builder()
                    .userId(userId)
                    .token(request.getToken())
                    .build();

            tokenRepository.save(token);
        }

        return "Token registered";
    }
}
