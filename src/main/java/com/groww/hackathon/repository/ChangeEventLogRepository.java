package com.groww.hackathon.repository;

import com.groww.hackathon.model.ChangeEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChangeEventLogRepository extends JpaRepository<ChangeEventLog, Long> {
    Optional<ChangeEventLog> findTopByUserIdAndSymbolOrderByOccurredAtDesc(String userId, String symbol);
    List<ChangeEventLog> findByUserIdOrderByOccurredAtDesc(String userId);
}