package com.fuelnet.fuelnet.services;

import com.fuelnet.fuelnet.dto.AdminRegisterRequest;
import com.fuelnet.fuelnet.dto.LoginRequestDto;
import com.fuelnet.fuelnet.dto.LoginResponseDto;
import com.fuelnet.fuelnet.dto.SignupRequestDto;
import com.fuelnet.fuelnet.dto.SignupResponseDto;
import com.fuelnet.fuelnet.enums.PendingUserType;
import com.fuelnet.fuelnet.enums.UserRole;
import com.fuelnet.fuelnet.models.AppUser;
import com.fuelnet.fuelnet.models.PendingUser;
import com.fuelnet.fuelnet.models.StationUser;
import com.fuelnet.fuelnet.repositories.IAppUserRepository;
import com.fuelnet.fuelnet.repositories.IPendingUsersRepository;
import com.fuelnet.fuelnet.repositories.IStationUserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final IStationUserRepository stationUserRepository;
    private final IAppUserRepository appUserRepository;
    private final IPendingUsersRepository pendingUsersRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Logger logger = Logger.getLogger("auth_service");

    public SignupResponseDto registerPendingUserClient(SignupRequestDto request) {
        if (stationUserRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.info("User is already registered");
            throw new RuntimeException("Email is already sign up");
        }
        String token = UUID.randomUUID().toString();
        PendingUserType pendingType = PendingUserType.CUSTOMER;
        switch (request.getRole()) {
            case "Usuario": {
                pendingType = PendingUserType.CUSTOMER;
                break;
            }
            case "Administrador de estacion": {
                pendingType = PendingUserType.STATION_ADMIN;
                break;
            }
        }

        PendingUser pendingUser = PendingUser.builder()
                .email(request.getEmail())
                .type(pendingType)
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .address(request.getAddress())
                .birthDate(LocalDate.parse(request.getBirthday()))
                .gender(request.getGender())
                .token(token)
                .tokenExpiration(LocalDateTime.now().plusHours(24))
                .build();

        pendingUsersRepository.save(pendingUser);

        if (pendingType == PendingUserType.STATION_ADMIN) {
            emailService.sendPlatformAdminReviewMail();
            emailService.sendWaitingForRevision(pendingUser.getEmail());
            logger.info("Admin has been register, needs admin to confirm email");
        } else {
            emailService.sendEmailVerification(pendingUser.getEmail(), token);
            logger.info("User has been register, needs to confirm email");
        }

        return new SignupResponseDto("User has been register");
    }

    public boolean createStationUserFromPending(PendingUser pendingUser) {
        StationUser user = StationUser.builder()
                .email(pendingUser.getEmail())
                .name(pendingUser.getName())
                .username(pendingUser.getName())
                .password(pendingUser.getPassword())
                .address(pendingUser.getAddress())
                .birthDate(pendingUser.getBirthDate())
                .gender(pendingUser.getGender())
                .role(UserRole.STATION_ADMIN)
                .build();

        try {
            stationUserRepository.save(user);
            pendingUsersRepository.delete(pendingUser);
            switch (user.getRole()) {
                case STATION_ADMIN: {
                    emailService.sendApproveByAdmin(user.getEmail());
                    break;
                }
                case EMPLOYEE: {
                    break;
                }
                case PLATFORM_ADMIN: {
                    throw new Exception("Cant create platform admin, from this resource");
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to create user");
            return false;
        }
        return true;
    }

    public boolean createAppUserFromPending(PendingUser pendingUser) {
        AppUser user = AppUser.builder()
                .email(pendingUser.getEmail())
                .name(pendingUser.getName())
                .password(pendingUser.getPassword())
                .address(pendingUser.getAddress())
                .birthDate(pendingUser.getBirthDate())
                .gender(pendingUser.getGender())
                .build();

        try {
            appUserRepository.save(user);
            pendingUsersRepository.delete(pendingUser);
            emailService.sendSuccessUserVerification(user.getEmail());
        } catch (Exception e) {
            logger.severe("Failed to create user");
            return false;
        }
        return true;
    }

    public boolean change_password(StationUser user, String oldPassword, String newPassword) {
        try {
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return false;
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            stationUserRepository.save(user);
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public LoginResponseDto login(LoginRequestDto request) {

        Optional<StationUser> stationUserOpt = stationUserRepository.findByEmail(request.getEmail());

        if (stationUserOpt.isPresent()) {
            StationUser user = stationUserOpt.get();

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("Password incorrect");
            }

            String token = jwtService.generateToken(user);

            return new LoginResponseDto(
                    token,
                    user.getRole().name());
        }

        Optional<AppUser> appUserOpt = appUserRepository.findByEmail(request.getEmail());

        if (appUserOpt.isPresent()) {
            AppUser user = appUserOpt.get();

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("Password incorrect");
            }

            String token = jwtService.generateTokenForAppUser(user);

            return new LoginResponseDto(
                    token,
                    "APP_USER");
        }

        throw new RuntimeException("User not found");
    }

    public SignupResponseDto registerStationAdmin(
            AdminRegisterRequest request) {
        PendingUser pendingAdmin = pendingUsersRepository.findById(request.getPendingUserId()).orElseThrow();

        if (stationUserRepository.findByEmail(pendingAdmin.getEmail()).isPresent()) {
            logger.info("User is already registered");
            throw new RuntimeException("Email is already sign up");
        }

        if (!request.getAccepted()) {
            emailService.sendRejectedByAdmin(pendingAdmin.getEmail(), "No fue aceptado porque no tiene permisos");
            pendingUsersRepository.delete(pendingAdmin);
            return new SignupResponseDto("User has been rejected");
        }

        StationUser user = StationUser.builder()
                .name(pendingAdmin.getName())
                .address(pendingAdmin.getAddress())
                .gender(pendingAdmin.getGender())
                .birthDate(pendingAdmin.getBirthDate())
                .username(pendingAdmin.getName())
                .email(pendingAdmin.getEmail())
                .password(pendingAdmin.getPassword())
                .role(UserRole.STATION_ADMIN)
                .build();

        stationUserRepository.save(user);
        pendingUsersRepository.delete(pendingAdmin);
        emailService.sendApproveByAdmin(user.getEmail());
        logger.info("Station admin has been registered");
        return new SignupResponseDto("User has been registered");
    }
}
