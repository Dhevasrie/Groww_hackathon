package com.groww.hackathon.repository;

import com.groww.hackathon.model.MarketTick;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketTickRepository extends JpaRepository<MarketTick, Long> {
    // most recent tick for a symbol
    Optional<MarketTick> findFirstBySymbolOrderByTimestampDesc(String symbol);

    // recent history window for a symbol, used to seed/refresh SymbolStats
    List<MarketTick> findTop50BySymbolOrderByTimestampDesc(String symbol);

    List<String> findDistinctSymbolBy();
}