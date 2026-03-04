package com.fuelnet.fuelnet.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fuelnet.fuelnet.enums.FuelType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private FuelType fuelType;

    private Double price;

    @ManyToOne
    @JoinColumn(name = "station_id")
    @JsonIgnore
    private Station station;
}
