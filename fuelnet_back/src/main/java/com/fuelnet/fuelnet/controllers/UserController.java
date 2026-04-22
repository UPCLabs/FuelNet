package com.fuelnet.fuelnet.controllers;

import com.fuelnet.fuelnet.models.StationUser;

import com.fuelnet.fuelnet.services.AuthService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal StationUser user,
            @RequestBody Map<String, String> request) {
        boolean status = authService.change_password(user, request.get("oldPassword"), request.get("newPassword"));

        if (!status) {
            return ResponseEntity.badRequest().body(Map.of("message", "Passwords don't match"));
        }

        return ResponseEntity.ok(Map.of("message", "Password has been changed sucessfully"));
    }
}
