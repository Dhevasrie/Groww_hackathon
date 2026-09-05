package com.groww.hackathon.service;

import com.groww.hackathon.model.ChangeContext;
import com.groww.hackathon.model.ChangeSeverity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Distinguishes "this stock moved because the whole market moved" from
 * "this stock moved on its own." A watchlist where 5 of 6 holdings jump at
 * once isn't 5 signals — it's one market-wide event and, at most, one real
 * outlier. Feeding raw per-symbol severities straight to the user hides
 * that distinction; this pass restores it.
 *
 * Pure function, no persistence — called once per getWatchlistView() scan
 * with whatever severities ChangeDetectionService already produced.
 */
@Service
public class MarketContextService {

    // if at least this fraction of the watchlist is NOTABLE/SIGNIFICANT at once,
    // it's more likely a market/sector event than N independent stories
    private static final double MARKET_WIDE_PROPORTION = 0.5;
    private static final int MIN_MOVERS_FOR_MARKET_WIDE = 3;
    // of the movers, how dominant one direction has to be to call it "correlated"
    private static final double DIRECTIONAL_CORRELATION = 0.7;
    // even inside a market-wide event, a move this far past the pack's average
    // is still its own story, not just riding the trend
    private static final double OUTLIER_MAGNITUDE_MULTIPLE = 1.8;

    public record RawChange(String symbol, ChangeSeverity severity, Double changePercent) {}

    public record ContextualizedChange(String symbol, ChangeContext context) {}

    private record MarketState(boolean marketWide, int dominantSign, double avgDominantMagnitude) {}

    public List<ContextualizedChange> annotate(List<RawChange> changes) {
        List<RawChange> movers = changes.stream().filter(this::isMover).toList();
        MarketState state = computeMarketState(changes, movers);

        return changes.stream()
                .map(c -> new ContextualizedChange(c.symbol(), classify(c, state)))
                .toList();
    }

    private ChangeContext classify(RawChange change, MarketState state) {
        if (!isMover(change)) return ChangeContext.NONE;
        if (!state.marketWide()) return ChangeContext.ISOLATED;

        double pct = change.changePercent() == null ? 0.0 : change.changePercent();
        int sign = pct >= 0 ? 1 : -1;
        if (sign != state.dominantSign()) {
            return ChangeContext.ISOLATED; // moving against the trend — the real story
        }
        boolean farAheadOfThePack = Math.abs(pct) > state.avgDominantMagnitude() * OUTLIER_MAGNITUDE_MULTIPLE;
        return farAheadOfThePack ? ChangeContext.ISOLATED : ChangeContext.MARKET_WIDE;
    }

    private MarketState computeMarketState(List<RawChange> all, List<RawChange> movers) {
        if (all.isEmpty() || movers.size() < MIN_MOVERS_FOR_MARKET_WIDE) {
            return new MarketState(false, 0, 0);
        }

        double proportion = (double) movers.size() / all.size();
        if (proportion < MARKET_WIDE_PROPORTION) {
            return new MarketState(false, 0, 0);
        }

        long positive = movers.stream()
                .filter(c -> (c.changePercent() == null ? 0.0 : c.changePercent()) >= 0)
                .count();
        long negative = movers.size() - positive;
        double dominantShare = Math.max(positive, negative) / (double) movers.size();
        if (dominantShare < DIRECTIONAL_CORRELATION) {
            return new MarketState(false, 0, 0);
        }

        int dominantSign = positive >= negative ? 1 : -1;
        double avgMagnitude = movers.stream()
                .mapToDouble(c -> c.changePercent() == null ? 0.0 : c.changePercent())
                .filter(pct -> (pct >= 0 ? 1 : -1) == dominantSign)
                .map(Math::abs)
                .average().orElse(0);

        return new MarketState(true, dominantSign, avgMagnitude);
    }

    private boolean isMover(RawChange change) {
        return change.severity() == ChangeSeverity.NOTABLE || change.severity() == ChangeSeverity.SIGNIFICANT;
    }
}