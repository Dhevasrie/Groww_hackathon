package com.groww.hackathon.dto;

import com.groww.hackathon.model.ChangeContext;
import com.groww.hackathon.model.ChangeSeverity;
import com.groww.hackathon.model.DataFreshness;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class WatchlistItemView {
    private String symbol;
    private Double currentPrice;
    private Double lastSeenPrice;   // null if never seen before
    private Double percentChange;   // null if no baseline yet
    private Double zScore;          // null if no baseline yet
    private ChangeSeverity severity;
    private DataFreshness freshness;
    private Instant lastUpdated;
    private String message;         // human-readable, this is what you actually show in the UI
    private ChangeContext context;  // MARKET_WIDE / ISOLATED / NONE — added for feature #1
}