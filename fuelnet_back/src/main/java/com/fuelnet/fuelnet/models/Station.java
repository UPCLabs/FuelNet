package com.fuelnet.fuelnet.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String address;

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FuelPrice> fuelPrices;
}
