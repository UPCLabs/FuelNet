package com.fuelnet.fuelnet.dto;

import java.util.List;
import lombok.Data;

@Data
public class StationCreationRequestDto {

    private String name;
    private String address;
    private List<FuelPriceDto> fuels;
}
