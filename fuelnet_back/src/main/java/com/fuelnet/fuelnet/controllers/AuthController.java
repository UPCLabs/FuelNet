package com.fuelnet.fuelnet.controllers;

import com.fuelnet.fuelnet.dto.CreateStationAdminRequest;
import com.fuelnet.fuelnet.dto.LoginRequestDto;
import com.fuelnet.fuelnet.dto.LoginResponseDto;
import com.fuelnet.fuelnet.dto.SignupRequestDto;
import com.fuelnet.fuelnet.dto.SignupResponseDto;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final IPendingUsersRepository pendingUsersRepository;
    private final IUserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignupRequestDto request) {
        try {
            SignupResponseDto response = authService.registerClient(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/activate")
    public ResponseEntity<?> activate(@RequestParam String token) {
        PendingUser pendingUser = pendingUsersRepository
                .findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (pendingUser.getTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        User user = User.builder()
                .email(pendingUser.getEmail())
                .name(pendingUser.getName())
                .password(pendingUser.getPassword())
                .address(pendingUser.getAddress())
                .birthDate(pendingUser.getBirthDate())
                .gender(pendingUser.getGender())
                .role(pendingUser.getRoleRequested())
                .build();

        userRepository.save(user);
        pendingUsersRepository.delete(pendingUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Tu cuenta ya fue verificada, inicia sesión"));
    }

    @PostMapping("/register-station-admin")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<?> registerStationAdmin(
            @RequestBody CreateStationAdminRequest request) {
        try {
            SignupResponseDto response = authService.registerStationAdmin(
                    request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {
        try {
            LoginResponseDto response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
