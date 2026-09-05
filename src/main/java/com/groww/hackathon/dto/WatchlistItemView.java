package com.groww.hackathon.dto;

import com.groww.hackathon.model.ChangeContext;
import com.groww.hackathon.model.ChangeSeverity;
import com.groww.hackathon.model.DataFreshness;
import lombok.AllArgsConstructor;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class WatchlistItemView {
    private String symbol;
    private Double currentPrice;
    private Double lastSeenPrice;
    private Double percentChange;
    @JsonProperty("zScore")
    private Double zScore;
    private ChangeSeverity severity;
    private DataFreshness freshness;
    private Instant lastUpdated;
    private String message;
    private ChangeContext context;
    private double thresholdMultiplier; // new — powers the explainability panel

}