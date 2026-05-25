package com.fuelnet.fuelnet.controllers;

import com.fuelnet.fuelnet.dto.AdminRegisterRequest;
import com.fuelnet.fuelnet.dto.AppUserMeDto;
import com.fuelnet.fuelnet.dto.CreateEmployeeDto;
import com.fuelnet.fuelnet.dto.LoginRequestDto;
import com.fuelnet.fuelnet.dto.LoginResponseDto;
import com.fuelnet.fuelnet.dto.SignupRequestDto;
import com.fuelnet.fuelnet.dto.SignupResponseDto;
import com.fuelnet.fuelnet.dto.StationUserMeDto;
import com.fuelnet.fuelnet.enums.PendingUserType;
import com.fuelnet.fuelnet.models.AppUser;
import com.fuelnet.fuelnet.models.PendingUser;
import com.fuelnet.fuelnet.models.StationUser;
import com.fuelnet.fuelnet.repositories.IPendingUsersRepository;
import com.fuelnet.fuelnet.services.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    private StationUserMeDto toStationDto(StationUser user) {
        return StationUserMeDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .username(user.getUsername())
                .address(user.getAddress())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .role(user.getRole())
                .stationId(user.getStation() != null ? user.getStation().getId() : null)
                .permissions(user.getPermissions() != null
                        ? user.getPermissions()
                                .stream()
                                .map(Enum::name)
                                .toList()
                        : List.of())
                .build();
    }

    private AppUserMeDto toAppUserDto(AppUser user) {
        return AppUserMeDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .address(user.getAddress())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Object user) {

        if (user instanceof StationUser stationUser) {
            return ResponseEntity.ok(toStationDto(stationUser));
        }

        if (user instanceof AppUser appUser) {
            return ResponseEntity.ok(toAppUserDto(appUser));
        }

        return ResponseEntity.status(401).build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignupRequestDto request) {
        try {
            SignupResponseDto response = authService.registerPendingUserClient(request);
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

        if (pendingUser.getType() == PendingUserType.CUSTOMER) {
            authService.createAppUserFromPending(pendingUser);
        } else {
            authService.createStationUserFromPending(pendingUser);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Tu cuenta ya fue verificada, inicia sesión"));
    }

    @PostMapping("/admin_aceptation")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<?> registerStationAdmin(
            @RequestBody AdminRegisterRequest request) {
        try {
            SignupResponseDto response = authService.registerStationAdmin(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {
        try {
            LoginResponseDto response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/create-employee")
    @PreAuthorize("hasRole('STATION_ADMIN')")
    public ResponseEntity<StationUser> createUser(
            @Valid @RequestBody CreateEmployeeDto request,
            @AuthenticationPrincipal StationUser admin) {
        return ResponseEntity.ok(
                authService.createEmployee(request, admin));
    }
}
