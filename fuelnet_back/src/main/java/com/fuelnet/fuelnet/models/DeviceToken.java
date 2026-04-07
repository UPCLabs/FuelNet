package com.fuelnet.fuelnet.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "device_notifications_token")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(unique = true)
    private String token;
}
