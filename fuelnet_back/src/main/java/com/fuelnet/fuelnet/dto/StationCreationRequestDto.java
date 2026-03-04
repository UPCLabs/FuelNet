package com.fuelnet.fuelnet.dto;

import lombok.Data;
import java.util.List;

@Data
public class StationCreationRequestDto {
    private String name;
    private String address;
    private List<FuelPriceDto> fuels;
}
