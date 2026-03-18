package com.fuelnet.fuelnet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStationAdminRequest {

    private String name;
    private String email;
    private String password;

    @JsonProperty("station_id")
    private Long stationId;
}
