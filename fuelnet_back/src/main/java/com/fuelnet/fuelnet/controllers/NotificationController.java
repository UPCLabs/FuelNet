package com.fuelnet.fuelnet.controllers;

import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.fuelnet.fuelnet.dto.RegisterNotificationTokenDto;
import com.fuelnet.fuelnet.models.DeviceToken;
import com.fuelnet.fuelnet.models.User;
import com.fuelnet.fuelnet.repositories.IDeviceTokenRepository;

import lombok.*;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final IDeviceTokenRepository tokenRepository;

    @PostMapping("/register")
    public String registerToken(@RequestBody RegisterNotificationTokenDto request) {

        User user = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Optional<DeviceToken> existing = tokenRepository.findByToken(request.getToken());

        if (existing.isPresent()) {
            DeviceToken token = existing.get();
            token.setUserId(user.getId());
            tokenRepository.save(token);
        } else {
            DeviceToken token = DeviceToken.builder()
                    .userId(user.getId())
                    .token(request.getToken())
                    .build();

            tokenRepository.save(token);
        }

        return "Token registered";
    }
}
