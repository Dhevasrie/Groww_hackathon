package com.groww.hackathon.service;

import com.groww.hackathon.model.*;
import com.groww.hackathon.repository.MarketTickRepository;
import com.groww.hackathon.repository.SymbolStatsRepository;
import com.groww.hackathon.repository.UserSymbolSensitivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChangeDetectionService {

    private static final Duration STALE_THRESHOLD = Duration.ofSeconds(20);
    private static final double CONFLICT_PRICE_DIFF_THRESHOLD = 0.003;
    private static final double SIGNIFICANT_Z = 2.0;
    private static final double NOTABLE_Z = 1.0;
    private static final long MIN_SAMPLES_FOR_RELIABLE_STATS = 5;
    private static final double FALLBACK_STDDEV = 0.01;
    private static final double DEFAULT_THRESHOLD_MULTIPLIER = 1.0;
    private static final double DISMISSAL_INCREASE_FACTOR = 1.15;
    private static final double MAX_THRESHOLD_MULTIPLIER = 3.0;

    private final MarketTickRepository tickRepository;
    private final SymbolStatsRepository statsRepository;
    private final UserSymbolSensitivityRepository sensitivityRepository;

    public Optional<MarketTick> latestTick(String symbol) {
        return tickRepository.findFirstBySymbolOrderByTimestampDesc(symbol);
    }

    public DataFreshness assessFreshness(MarketTick latest, String symbol) {
        if (latest == null) return DataFreshness.STALE;
        if (isConflicted(symbol)) return DataFreshness.CONFLICTED;

        boolean isStale = Duration.between(latest.getTimestamp(), Instant.now())
                .compareTo(STALE_THRESHOLD) > 0;
        return isStale ? DataFreshness.STALE : DataFreshness.FRESH;
    }

    private boolean isConflicted(String symbol) {
        List<MarketTick> recent = tickRepository.findTop50BySymbolOrderByTimestampDesc(symbol);
        if (recent.size() < 2) return false;

        MarketTick a = recent.get(0);
        MarketTick b = recent.get(1);
        if (a.getSourceId().equals(b.getSourceId())) return false;

        double diff = Math.abs(a.getPrice() - b.getPrice()) / b.getPrice();
        return diff > CONFLICT_PRICE_DIFF_THRESHOLD;
    }

    public double resolveThresholdMultiplier(String userId, String symbol) {
        return sensitivityRepository.findByUserIdAndSymbol(userId, symbol)
                .map(UserSymbolSensitivity::getThresholdMultiplier)
                .orElse(DEFAULT_THRESHOLD_MULTIPLIER);
    }

    @Transactional
    public void recordDismissal(String userId, String symbol) {
        UserSymbolSensitivity sensitivity = sensitivityRepository.findByUserIdAndSymbol(userId, symbol)
                .orElseGet(() -> {
                    UserSymbolSensitivity fresh = new UserSymbolSensitivity();
                    fresh.setUserId(userId);
                    fresh.setSymbol(symbol);
                    fresh.setThresholdMultiplier(DEFAULT_THRESHOLD_MULTIPLIER);
                    fresh.setDismissCount(0);
                    return fresh;
                });

        double raised = sensitivity.getThresholdMultiplier() * DISMISSAL_INCREASE_FACTOR;
        sensitivity.setThresholdMultiplier(Math.min(raised, MAX_THRESHOLD_MULTIPLIER));
        sensitivity.setDismissCount(sensitivity.getDismissCount() + 1);
        sensitivity.setUpdatedAt(Instant.now());

        sensitivityRepository.save(sensitivity);
    }

    /** Powers the sensitivity settings page — every symbol this user has ever dismissed. */
    public List<UserSymbolSensitivity> getSensitivities(String userId) {
        return sensitivityRepository.findByUserId(userId);
    }

    /**
     * Resets a symbol's learned sensitivity back to default (1.0, 0 dismissals)
     * without deleting the row — the settings page can then show "reset" as a
     * visible state rather than the symbol just disappearing from the list.
     */
    @Transactional
    public void resetSensitivity(String userId, String symbol) {
        sensitivityRepository.findByUserIdAndSymbol(userId, symbol).ifPresent(sensitivity -> {
            sensitivity.setThresholdMultiplier(DEFAULT_THRESHOLD_MULTIPLIER);
            sensitivity.setDismissCount(0);
            sensitivity.setUpdatedAt(Instant.now());
            sensitivityRepository.save(sensitivity);
        });
    }

    public ChangeResult classify(String symbol, double currentPrice, Double lastSeenPrice,
                                 double thresholdMultiplier) {
        if (lastSeenPrice == null) {
            return new ChangeResult(ChangeSeverity.NEW, null, null,
                    "New to your watchlist — no baseline yet");
        }

        double percentChange = (currentPrice - lastSeenPrice) / lastSeenPrice;

        SymbolStats stats = statsRepository.findById(symbol).orElse(null);
        boolean hasReliableStats = stats != null && stats.getSampleCount() >= MIN_SAMPLES_FOR_RELIABLE_STATS;
        double stddev = hasReliableStats ? Math.max(stats.getStddevReturn(), 1e-6) : FALLBACK_STDDEV;

        double zScore = percentChange / stddev;
        double absZ = Math.abs(zScore);

        double significantCutoff = SIGNIFICANT_Z * thresholdMultiplier;
        double notableCutoff = NOTABLE_Z * thresholdMultiplier;

        ChangeSeverity severity;
        String message;
        if (absZ >= significantCutoff) {
            severity = ChangeSeverity.SIGNIFICANT;
            message = String.format("Unusual move for %s: %.2f%% is %.1fx its normal volatility",
                    symbol, percentChange * 100, absZ);
        } else if (absZ >= notableCutoff) {
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