package com.fuelnet.fuelnet.config;

import com.fuelnet.fuelnet.enums.UserRole;
import com.fuelnet.fuelnet.models.User;
import com.fuelnet.fuelnet.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "admin@fuelnet.com";

        boolean exists = userRepository.existsByEmail(adminEmail);

        if (!exists) {
            User admin = User.builder()
                .name("admin")
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
