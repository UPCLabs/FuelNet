package com.fuelnet.fuelnet.interfaces;

import com.fuelnet.fuelnet.dto.LoginResponseDto;
import com.fuelnet.fuelnet.dto.LoginRequestDto;
import com.fuelnet.fuelnet.dto.SignupRequestDto;
import com.fuelnet.fuelnet.dto.SignupResponseDto;

public interface IAuthService {
    SignupResponseDto register(SignupRequestDto request);

    LoginResponseDto login(LoginRequestDto request);
}
