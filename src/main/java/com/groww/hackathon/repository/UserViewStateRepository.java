package com.groww.hackathon.repository;

import com.groww.hackathon.model.UserViewState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserViewStateRepository extends JpaRepository<UserViewState, Long> {
    Optional<UserViewState> findByUserIdAndSymbol(String userId, String symbol);
}