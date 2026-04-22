package com.fuelnet.fuelnet.models;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_users")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;
    private String name;

    private String address;
    private LocalDate birthDate;
    private String gender;
}
