package com.groww.hackathon.service;

import com.groww.hackathon.model.*;
import com.groww.hackathon.repository.MarketTickRepository;
import com.groww.hackathon.repository.SymbolStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChangeDetectionService {

    // 4x the 5s ingestion interval — one missed cycle is noise, four in a row is a real problem
    private static final Duration STALE_THRESHOLD = Duration.ofSeconds(20);
    private static final double CONFLICT_PRICE_DIFF_THRESHOLD = 0.003; // 0.3% disagreement between sources
    private static final double SIGNIFICANT_Z = 2.0;
    private static final double NOTABLE_Z = 1.0;

    private final MarketTickRepository tickRepository;
    private final SymbolStatsRepository statsRepository;

    public Optional<MarketTick> latestTick(String symbol) {
        return tickRepository.findFirstBySymbolOrderByTimestampDesc(symbol);
    }

    public DataFreshness assessFreshness(MarketTick latest, String symbol) {
        if (latest == null) return DataFreshness.STALE; // no data at all yet
        if (isConflicted(symbol)) return DataFreshness.CONFLICTED;

        boolean isStale = Duration.between(latest.getTimestamp(), Instant.now())
                .compareTo(STALE_THRESHOLD) > 0;
        return isStale ? DataFreshness.STALE : DataFreshness.FRESH;
    }

    // looks at the 2 most recent ticks; if they came from different sources and
    // disagree by more than the threshold, flag it rather than silently picking one
    private boolean isConflicted(String symbol) {
        List<MarketTick> recent = tickRepository.findTop50BySymbolOrderByTimestampDesc(symbol);
        if (recent.size() < 2) return false;

        MarketTick a = recent.get(0);
        MarketTick b = recent.get(1);
        if (a.getSourceId().equals(b.getSourceId())) return false;

        double diff = Math.abs(a.getPrice() - b.getPrice()) / b.getPrice();
        return diff > CONFLICT_PRICE_DIFF_THRESHOLD;
    }

    public ChangeResult classify(String symbol, double currentPrice, Double lastSeenPrice) {
        if (lastSeenPrice == null) {
            return new ChangeResult(ChangeSeverity.NEW, null, null,
                    "New to your watchlist — no baseline yet");
        }

        double percentChange = (currentPrice - lastSeenPrice) / lastSeenPrice;

        SymbolStats stats = statsRepository.findById(symbol).orElse(null);
        // floor prevents divide-by-near-zero for a symbol with almost no history yet
        double stddev = (stats != null) ? Math.max(stats.getStddevReturn(), 1e-6) : 0.01;

        double zScore = percentChange / stddev;
        double absZ = Math.abs(zScore);

        ChangeSeverity severity;
        String message;
        if (absZ >= SIGNIFICANT_Z) {
            severity = ChangeSeverity.SIGNIFICANT;
            message = String.format("Unusual move for %s: %.2f%% is %.1fx its normal volatility",
                    symbol, percentChange * 100, absZ);
        } else if (absZ >= NOTABLE_Z) {
            severity = ChangeSeverity.NOTABLE;
            message = String.format("%.2f%% change — somewhat above %s's normal range",
                    percentChange * 100, symbol);
        } else {
            severity = ChangeSeverity.QUIET;
            message = String.format("%.2f%% change — within normal range for %s",
                    percentChange * 100, symbol);
        }

        return new ChangeResult(severity, percentChange, zScore, message);
    }

    public record ChangeResult(ChangeSeverity severity, Double percentChange, Double zScore, String message) {}
}