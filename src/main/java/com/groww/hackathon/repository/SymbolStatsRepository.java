package com.groww.hackathon.repository;

import com.groww.hackathon.model.SymbolStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SymbolStatsRepository extends JpaRepository<SymbolStats, String> {
    // symbol is the @Id, so findById(symbol) already works out of the box
}