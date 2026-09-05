package com.groww.hackathon.service;

import com.groww.hackathon.model.ChangeSeverity;
import com.groww.hackathon.model.MarketTick;
import com.groww.hackathon.repository.MarketTickRepository;
import com.groww.hackathon.repository.SymbolStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Replays ChangeDetectionService's own classify() over a symbol's stored
 * tick history so the detail page shows the exact same severity logic used
 * live — not a separate, second definition of "significant" that could
 * drift out of sync with the one on the main watchlist.
 *
 * Note: this uses the symbol's *current* SymbolStats row for every point in
 * the history, not a point-in-time snapshot of volatility as it stood back
 * then. That's a deliberate simplification for the hackathon timeline —
 * worth naming explicitly in the README's "what I'd do with more time".
 */
@Service
@RequiredArgsConstructor
public class SymbolDetailService {

    private static final int HISTORY_LIMIT = 200;

    private final MarketTickRepository tickRepository;
    private final SymbolStatsRepository statsRepository;
    private final ChangeDetectionService changeDetectionService;

    public record TickPoint(Instant timestamp, double price, Long volume, String sourceId,
                            ChangeSeverity severity, Double percentChange, Double zScore) {}

    /** Newest first — matches how every other list in this app is ordered. */
    public List<TickPoint> getHistory(String symbol) {
        List<MarketTick> newestFirst = tickRepository.findTop200BySymbolOrderByTimestampDesc(symbol);

        List<MarketTick> chronological = new ArrayList<>(newestFirst);
        chronological.sort(Comparator.comparing(MarketTick::getTimestamp));

        List<TickPoint> points = new ArrayList<>();
        Double previousPrice = null;

        for (MarketTick tick : chronological) {
            ChangeDetectionService.ChangeResult result =
                    changeDetectionService.classify(symbol, tick.getPrice(), previousPrice);

            points.add(new TickPoint(tick.getTimestamp(), tick.getPrice(), tick.getVolume(),
                    tick.getSourceId(), result.severity(), result.percentChange(), result.zScore()));

            previousPrice = tick.getPrice();
        }

        Collections.reverse(points); // newest first for display
        return points;
    }
}