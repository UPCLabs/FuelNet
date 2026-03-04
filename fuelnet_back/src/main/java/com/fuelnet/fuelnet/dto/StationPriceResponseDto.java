package com.fuelnet.fuelnet.dto;

import java.util.List;
import lombok.*;

@Data
@AllArgsConstructor
public class StationPriceResponseDto {

    private Long stationId;
    private String stationName;
    private List<FuelPriceDto> fuels;
}
