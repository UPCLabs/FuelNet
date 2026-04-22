package com.fuelnet.fuelnet.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fuelnet.fuelnet.enums.PendingUserType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "pending_users")
public class PendingUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String address;

    @Column
    private LocalDate birthDate;

    @Column
    private String gender;

    private String token;
    private LocalDateTime tokenExpiration;

    @Enumerated(EnumType.STRING)
    private PendingUserType type;
}
