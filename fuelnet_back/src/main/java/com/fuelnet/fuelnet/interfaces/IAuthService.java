package com.fuelnet.fuelnet.interfaces;

import com.fuelnet.fuelnet.dto.AuthResponse;
import com.fuelnet.fuelnet.dto.LoginRequest;
import com.fuelnet.fuelnet.dto.SignupRequest;

public interface IAuthService {
    AuthResponse register(SignupRequest request);

    AuthResponse login(LoginRequest request);
}
