package com.fuelnet.fuelnet.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FuelPriceDto {

    private String type;
    private Double price;
}
