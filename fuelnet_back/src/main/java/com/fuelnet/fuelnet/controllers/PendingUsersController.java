package com.fuelnet.fuelnet.controllers;

import com.fuelnet.fuelnet.dto.PendingUserDto;
import com.fuelnet.fuelnet.enums.UserRole;
import com.fuelnet.fuelnet.models.PendingUser;
import com.fuelnet.fuelnet.repositories.IPendingUsersRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pending-users")
@RequiredArgsConstructor
public class PendingUsersController {

    private final IPendingUsersRepository pendingUsersRepository;

    private PendingUserDto toDto(PendingUser user) {
        return PendingUserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .address(user.getAddress())
                .birthDate(user.getBirthDate().toString())
                .gender(user.getGender())
                .roleRequested(user.getRoleRequested().name())
                .build();
    }

    @GetMapping("/get-all-pending")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<?> getAllPendingUsers() {
        List<PendingUserDto> dtoList = pendingUsersRepository
                .findAll()
                .stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/get-pending")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<?> getPendingUsers() {

        List<PendingUserDto> dtoList = pendingUsersRepository
                .findByRoleRequestedNot(UserRole.USER)
                .stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(dtoList);
    }

}
