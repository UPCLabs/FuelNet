package com.fuelnet.fuelnet.interfaces;

import com.fuelnet.fuelnet.dto.CreateStationAdminRequest;
import com.fuelnet.fuelnet.dto.LoginRequestDto;
import com.fuelnet.fuelnet.dto.LoginResponseDto;
import com.fuelnet.fuelnet.dto.SignupRequestDto;
import com.fuelnet.fuelnet.dto.SignupResponseDto;

public interface IAuthService {
    SignupResponseDto registerClient(SignupRequestDto request);
    SignupResponseDto registerStationAdmin(CreateStationAdminRequest request);

    LoginResponseDto login(LoginRequestDto request);
}
