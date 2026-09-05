package com.groww.hackathon.controller;

import com.groww.hackathon.service.feed.SimulatedMarketDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Backs the search-as-you-type box on the add-symbol form. Reads straight
 * from SimulatedMarketDataSource's known symbol set — no separate hardcoded
 * list to keep in sync.
 */
@RestController
@RequestMapping("/api/symbols")
@RequiredArgsConstructor
public class SymbolSearchController {

    private final SimulatedMarketDataSource marketDataSource;

    @GetMapping
    public List<String> search(@RequestParam(required = false, defaultValue = "") String query) {
        Set<String> known = marketDataSource.knownSymbols();
        String normalizedQuery = query.trim().toUpperCase();

        return known.stream()
                .filter(symbol -> normalizedQuery.isEmpty() || symbol.contains(normalizedQuery))
                .sorted()
                .collect(Collectors.toList());
    }
}