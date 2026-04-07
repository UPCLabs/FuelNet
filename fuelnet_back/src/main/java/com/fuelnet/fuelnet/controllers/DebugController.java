package com.fuelnet.fuelnet.controllers;

import org.springframework.web.bind.annotation.*;

import com.fuelnet.fuelnet.services.NotificationService;

import lombok.*;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {
    private final NotificationService fcmService;

    @GetMapping("/send-to-token")
    public String sendToToken(@RequestParam String token) throws Exception {
        return fcmService.sendToToken(
                token,
                "Test token 🚀",
                "Mensaje directo");
    }

    @GetMapping("/send-to-token-id")
    public String sendToTokenId(@RequestParam String userId) throws Exception {
        return fcmService.sendToUser(
                Long.parseLong(userId),
                "Test token 🚀",
                "Mensaje directo");
    }
}
