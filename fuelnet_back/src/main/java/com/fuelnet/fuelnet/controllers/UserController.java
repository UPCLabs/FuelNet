package com.fuelnet.fuelnet.controllers;

import com.fuelnet.fuelnet.dto.AdminRegisterRequest;
import com.fuelnet.fuelnet.dto.LoginRequestDto;
import com.fuelnet.fuelnet.dto.LoginResponseDto;
import com.fuelnet.fuelnet.dto.SignupRequestDto;
import com.fuelnet.fuelnet.dto.SignupResponseDto;
import com.fuelnet.fuelnet.dto.UserMeDto;
import com.fuelnet.fuelnet.models.PendingUser;
import com.fuelnet.fuelnet.models.User;
import com.fuelnet.fuelnet.repositories.IPendingUsersRepository;
import com.fuelnet.fuelnet.repositories.IUserRepository;
import com.fuelnet.fuelnet.services.AuthService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal User user,
            @RequestBody Map<String, String> request) {
        boolean status = authService.change_password(user, request.get("oldPassword"), request.get("newPassword"));

        if (!status) {
            return ResponseEntity.badRequest().body(Map.of("message", "Passwords don't match"));
        }

        return ResponseEntity.ok(Map.of("message", "Password has been changed sucessfully"));
    }
}
