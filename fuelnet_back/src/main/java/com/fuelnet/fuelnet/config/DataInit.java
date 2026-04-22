package com.fuelnet.fuelnet.config;

import com.fuelnet.fuelnet.enums.UserRole;
import com.fuelnet.fuelnet.models.StationUser;
import com.fuelnet.fuelnet.repositories.IStationUserRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {

    private final IStationUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "santiago.mendoza@santimendoza.com";

        boolean exists = userRepository.existsByEmail(adminEmail);

        if (!exists) {
            StationUser admin = StationUser.builder()
                    .name("admin")
                    .username("admin")
                    .address("Adminlandia")
                    .birthDate(LocalDate.parse("2006-01-20"))
                    .gender("Masculino")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin"))
                    .role(UserRole.PLATFORM_ADMIN)
                    .build();

            userRepository.save(admin);

            System.out.println("✅ Platform Admin created successfully");
        } else {
            System.out.println("ℹ️ Platform Admin already exists");
        }
    }
}
