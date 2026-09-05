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

    // 4x the 5s ingestion interval — one missed cycle is noise, four in a row is a real problem
    private static final Duration STALE_THRESHOLD = Duration.ofSeconds(20);
    private static final double CONFLICT_PRICE_DIFF_THRESHOLD = 0.003; // 0.3% disagreement between sources
    private static final double SIGNIFICANT_Z = 2.0;
    private static final double NOTABLE_Z = 1.0;

    // each dismissal raises the bar by 15%, capped so it can never require an
    // absurd move to ever flag again
    private static final double DISMISSAL_STEP = 1.15;
    private static final double MAX_MULTIPLIER = 3.0;

    private final MarketTickRepository tickRepository;
    private final SymbolStatsRepository statsRepository;
    private final UserSymbolSensitivityRepository sensitivityRepository;

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

    /** Original signature, unchanged behavior — default sensitivity (multiplier 1.0). */
    public ChangeResult classify(String symbol, double currentPrice, Double lastSeenPrice) {
        return classify(symbol, currentPrice, lastSeenPrice, 1.0);
    }

    /** Personalized variant — thresholdMultiplier scales how big a move has to be to count. */
    public ChangeResult classify(String symbol, double currentPrice, Double lastSeenPrice, double thresholdMultiplier) {
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

    /** 1.0 if the user has never adjusted sensitivity for this symbol. */
    public double resolveThresholdMultiplier(String userId, String symbol) {
        return sensitivityRepository.findByUserIdAndSymbol(userId, symbol)
                .map(UserSymbolSensitivity::getThresholdMultiplier)
                .orElse(1.0);
    }

    @Transactional
    public void recordDismissal(String userId, String symbol) {
        UserSymbolSensitivity sensitivity = sensitivityRepository.findByUserIdAndSymbol(userId, symbol)
                .orElseGet(() -> {
                    UserSymbolSensitivity fresh = new UserSymbolSensitivity();
                    fresh.setUserId(userId);
                    fresh.setSymbol(symbol);
                    return fresh;
                });

        sensitivity.setDismissCount(sensitivity.getDismissCount() + 1);
        double raised = sensitivity.getThresholdMultiplier() * DISMISSAL_STEP;
        sensitivity.setThresholdMultiplier(Math.min(raised, MAX_MULTIPLIER));
        sensitivity.setUpdatedAt(Instant.now());

        sensitivityRepository.save(sensitivity);
    }
    public List<UserSymbolSensitivity> getSensitivities(String userId) {
        return sensitivityRepository.findByUserId(userId);
    }

    /** Resets to default (1.0, dismissCount 0) but keeps the row — visible history
     *  of "this was reset" beats silently reverting to invisible/untracked. */
    @Transactional
    public void resetSensitivity(String userId, String symbol) {
        sensitivityRepository.findByUserIdAndSymbol(userId, symbol).ifPresent(s -> {
            s.setThresholdMultiplier(1.0);
            s.setDismissCount(0);
            s.setUpdatedAt(Instant.now());
            sensitivityRepository.save(s);
        });
    }
    public record ChangeResult(ChangeSeverity severity, Double percentChange, Double zScore, String message) {}
}