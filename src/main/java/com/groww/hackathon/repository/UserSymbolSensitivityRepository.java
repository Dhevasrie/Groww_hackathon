package com.groww.hackathon.repository;

import com.groww.hackathon.model.UserSymbolSensitivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSymbolSensitivityRepository extends JpaRepository<UserSymbolSensitivity, Long> {
    Optional<UserSymbolSensitivity> findByUserIdAndSymbol(String userId, String symbol);
}