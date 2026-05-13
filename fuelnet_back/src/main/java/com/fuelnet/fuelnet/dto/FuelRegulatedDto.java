package com.fuelnet.fuelnet.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuelRegulatedDto {
    private String circular;
    private String url;
    private Precios precios;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Precios {
        private Integer corriente;
        private Integer diesel;
    }
}
