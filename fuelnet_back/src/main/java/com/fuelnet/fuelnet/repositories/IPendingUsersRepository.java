package com.fuelnet.fuelnet.repositories;

import com.fuelnet.fuelnet.models.PendingUser;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IPendingUsersRepository extends JpaRepository<PendingUser, Long> {
    Optional<PendingUser> findByToken(String token);
}
