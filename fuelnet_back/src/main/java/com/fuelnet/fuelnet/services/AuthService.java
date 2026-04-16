package com.fuelnet.fuelnet.services;

import com.fuelnet.fuelnet.dto.AdminRegisterRequest;
import com.fuelnet.fuelnet.dto.LoginRequestDto;
import com.fuelnet.fuelnet.dto.LoginResponseDto;
import com.fuelnet.fuelnet.dto.SignupRequestDto;
import com.fuelnet.fuelnet.dto.SignupResponseDto;
import com.fuelnet.fuelnet.enums.UserRole;
import com.fuelnet.fuelnet.models.PendingUser;
import com.fuelnet.fuelnet.models.User;
import com.fuelnet.fuelnet.repositories.IPendingUsersRepository;
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
    private final IPendingUsersRepository pendingUsersRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Logger logger = Logger.getLogger("auth_service");

    public SignupResponseDto registerPendingUserClient(SignupRequestDto request) {
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

        pendingUsersRepository.save(pendingUser);

        if (role != UserRole.USER) {
            emailService.sendPlatformAdminReviewMail();
            emailService.sendWaitingForRevision(pendingUser.getEmail());
            logger.info("Admin has been register, needs admin to confirm email");
        } else {
            emailService.sendEmailVerification(pendingUser.getEmail(), token);
            logger.info("User has been register, needs to confirm email");
        }

        return new SignupResponseDto("User has been register");
    }

    public boolean createUserFromPending(PendingUser pendingUser) {
        User user = User.builder()
                .email(pendingUser.getEmail())
                .name(pendingUser.getName())
                .username(pendingUser.getName())
                .password(pendingUser.getPassword())
                .address(pendingUser.getAddress())
                .birthDate(pendingUser.getBirthDate())
                .gender(pendingUser.getGender())
                .role(pendingUser.getRoleRequested())
                .build();

        try {
            userRepository.save(user);
            pendingUsersRepository.delete(pendingUser);
            switch (user.getRole()) {
                case USER: {
                    emailService.sendSuccessUserVerification(user.getEmail());
                    break;
                }
                case STATION_ADMIN: {
                    emailService.sendApproveByAdmin(user.getEmail());
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

    public boolean change_password(User user, String newPassword) {
        try {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            return false;
        }
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
            AdminRegisterRequest request) {
        PendingUser pendingAdmin = pendingUsersRepository.findById(request.getPendingUserId()).orElseThrow();

        if (userRepository.findByEmail(pendingAdmin.getEmail()).isPresent()) {
            logger.info("User is already registered");
            throw new RuntimeException("Email is already sign up");
        }

        if (!request.getAccepted()) {
            emailService.sendRejectedByAdmin(pendingAdmin.getEmail(), "No fue aceptado porque no tiene permisos");
            pendingUsersRepository.delete(pendingAdmin);
            return new SignupResponseDto("User has been rejected");
        }

        User user = User.builder()
                .name(pendingAdmin.getName())
                .address(pendingAdmin.getAddress())
                .gender(pendingAdmin.getGender())
                .birthDate(pendingAdmin.getBirthDate())
                .username(pendingAdmin.getName())
                .email(pendingAdmin.getEmail())
                .password(pendingAdmin.getPassword())
                .role(UserRole.STATION_ADMIN)
                .build();

        userRepository.save(user);
        pendingUsersRepository.delete(pendingAdmin);
        emailService.sendApproveByAdmin(user.getEmail());
        logger.info("Station admin has been registered");
        return new SignupResponseDto("User has been registered");
    }
}
