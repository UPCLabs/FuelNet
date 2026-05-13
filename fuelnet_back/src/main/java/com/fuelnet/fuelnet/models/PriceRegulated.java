package com.fuelnet.fuelnet.models;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "price_regulated")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceRegulated {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String document;
    private String url;
    private Integer corriente;
    private Integer diesel;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;
}
