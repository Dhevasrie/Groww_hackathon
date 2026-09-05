package com.groww.hackathon.repository;

import com.groww.hackathon.model.DailySymbolStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailySymbolStatRepository extends JpaRepository<DailySymbolStat, Long> {
    Optional<DailySymbolStat> findByUserIdAndSymbolAndStatDate(String userId, String symbol, LocalDate statDate);
    List<DailySymbolStat> findByUserIdAndStatDate(String userId, LocalDate statDate);
}