package com.fuelnet.fuelnet.services;

import com.fuelnet.fuelnet.dto.LoginRequestDto;
import com.fuelnet.fuelnet.dto.LoginResponseDto;
import com.fuelnet.fuelnet.dto.SignupRequestDto;
import com.fuelnet.fuelnet.dto.SignupResponseDto;
import com.fuelnet.fuelnet.enums.UserRole;
import com.fuelnet.fuelnet.interfaces.IAuthService;
import com.fuelnet.fuelnet.models.User;
import com.fuelnet.fuelnet.repositories.IUserRepository;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Logger logger = Logger.getLogger("auth_service");

    @Override
    public SignupResponseDto register(SignupRequestDto request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.info("User is already registered");
            throw new RuntimeException("Email is already sign up");
        }

        User user = User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(UserRole.USER)
            .build();

        userRepository.save(user);
        logger.info("User has been register");
        return new SignupResponseDto("User has been register");
    }

    @Override
    public LoginResponseDto login(LoginRequestDto request) {
        User user = userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (
            !passwordEncoder.matches(request.getPassword(), user.getPassword())
        ) {
            throw new RuntimeException("Password incorrect");
        }

        logger.info("User login successful");
        String token = jwtService.generateToken(user);

        return new LoginResponseDto(token);
    }
}
