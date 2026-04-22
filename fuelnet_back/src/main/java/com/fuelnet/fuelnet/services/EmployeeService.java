package com.fuelnet.fuelnet.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fuelnet.fuelnet.dto.EmployeeResponseDto;
import com.fuelnet.fuelnet.dto.UpdateEmployeeDto;
import com.fuelnet.fuelnet.enums.UserRole;
import com.fuelnet.fuelnet.models.StationUser;
import com.fuelnet.fuelnet.repositories.IStationUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final IStationUserRepository stationUserRepository;

    public List<EmployeeResponseDto> getEmployees(StationUser admin) {

        return stationUserRepository.findByStationId(admin.getStation().getId())
                .stream()
                .filter(u -> u.getRole() != UserRole.STATION_ADMIN)
                .map(this::toDto)
                .toList();
    }

    public EmployeeResponseDto updateEmployee(Long id, UpdateEmployeeDto dto, StationUser admin) {

        StationUser user = stationUserRepository
                .findByIdAndStationId(id, admin.getStation().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(dto.getName());
        user.setPermissions(dto.getPermissions());

        stationUserRepository.save(user);

        return toDto(user);
    }

    public void deleteEmployee(Long id, StationUser admin) {

        StationUser user = stationUserRepository
                .findByIdAndStationId(id, admin.getStation().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        stationUserRepository.delete(user);
    }

    private EmployeeResponseDto toDto(StationUser user) {
        return EmployeeResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .permissions(
                        user.getPermissions() != null
                                ? user.getPermissions().stream().map(Enum::name).toList()
                                : List.of())
                .build();
    }
}
