package com.fuelnet.fuelnet.services;

import com.fuelnet.fuelnet.dto.CreateStationAdminRequest;
import com.fuelnet.fuelnet.dto.LoginRequestDto;
import com.fuelnet.fuelnet.dto.LoginResponseDto;
import com.fuelnet.fuelnet.dto.SignupRequestDto;
import com.fuelnet.fuelnet.dto.SignupResponseDto;
import com.fuelnet.fuelnet.enums.UserRole;
import com.fuelnet.fuelnet.interfaces.IAuthService;
import com.fuelnet.fuelnet.models.PendingUser;
import com.fuelnet.fuelnet.models.Station;
import com.fuelnet.fuelnet.models.User;
import com.fuelnet.fuelnet.repositories.IPendingUsersRepository;
import com.fuelnet.fuelnet.repositories.IStationRepository;
import com.fuelnet.fuelnet.repositories.IUserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final IUserRepository userRepository;
    private final IStationRepository stationRepository;
    private final IPendingUsersRepository pendingUsersRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Logger logger = Logger.getLogger("auth_service");

    public SignupResponseDto registerClient(SignupRequestDto request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.info("User is already registered");
            throw new RuntimeException("Email is already sign up");
        }
        String token = UUID.randomUUID().toString();
        UserRole role = UserRole.USER;
        switch (request.getRole()) {
            case "Usuario": {
                role = UserRole.USER;
                break;
            }
            case "Administrador de estacion": {
                role = UserRole.STATION_ADMIN;
                break;
            }
        }

        PendingUser pendingUser = PendingUser.builder()
                .email(request.getEmail())
                .roleRequested(role)
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .address(request.getAddress())
                .birthDate(LocalDate.parse(request.getBirthday()))
                .gender(request.getGender())
                .token(token)
                .tokenExpiration(LocalDateTime.now().plusHours(24))
                .build();

        // User user = User.builder()
        // .name(request.getName())
        // .email(request.getEmail())
        // .username(request.getUsername())
        // .password(passwordEncoder.encode(request.getPassword()))
        // .address(request.getAddress())
        // .birthDate(request.getBirthday() != null
        // ? LocalDate.parse(request.getBirthday())
        // : null)
        // .role(UserRole.valueOf(request.getRole()))
        // .gender(request.getGender())
        // .build();
        emailService.sendEmailVerification(pendingUser.getEmail(), token);

        pendingUsersRepository.save(pendingUser);
        logger.info("User has been register, needs to confirm email");
        return new SignupResponseDto("User has been register");
    }

    public LoginResponseDto login(LoginRequestDto request) {
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Password incorrect");
        }

        logger.info("User login successful");
        String token = jwtService.generateToken(user);

        return new LoginResponseDto(token, user.getRole().name());
    }

    public SignupResponseDto registerStationAdmin(
            CreateStationAdminRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.info("User is already registered");
            throw new RuntimeException("Email is already sign up");
        }

        Station station = stationRepository
                .findById(request.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found"));

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.STATION_ADMIN)
                .station(station)
                .build();

        userRepository.save(user);
        logger.info("Station admin has been registered");
        return new SignupResponseDto("User has been registered");
    }
}
