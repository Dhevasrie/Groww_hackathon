package com.groww.hackathon.service.feed;

import com.groww.hackathon.model.MarketTick;
import com.groww.hackathon.model.SymbolStats;
import com.groww.hackathon.repository.MarketTickRepository;
import com.groww.hackathon.repository.SymbolStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FeedIngestionScheduler {

    // hardcoded universe for the hackathon — a real system would drive this from
    // "the set of symbols currently on any user's watchlist," see README trade-offs
    private static final List<String> TRACKED_SYMBOLS =
            List.of("RELIANCE", "TCS", "INFY", "ZOMATO", "PAYTM", "HDFCBANK");

    private final MarketDataSource dataSource;
    private final MarketTickRepository tickRepository;
    private final SymbolStatsRepository statsRepository;

    @Scheduled(fixedRate = 5000) // every 5s — fast enough to demo, slow enough to read logs live
    public void ingest() {
        for (String symbol : TRACKED_SYMBOLS) {
            MarketDataSource.RawTick raw = dataSource.fetchTick(symbol);
            if (raw == null) {
                continue; // dropped cycle — the ABSENCE of a fresh tick is what staleness detection reads
            }

            MarketTick tick = new MarketTick();
            tick.setSymbol(symbol);
            tick.setPrice(raw.price());
            tick.setVolume(raw.volume());
            tick.setTimestamp(Instant.now());
            tick.setSourceId(raw.sourceId());
            tickRepository.save(tick);

            updateStats(symbol, raw.price());
        }
    }

    private void updateStats(String symbol, double newPrice) {
        SymbolStats stats = statsRepository.findById(symbol).orElseGet(() -> {
            SymbolStats s = new SymbolStats();
            s.setSymbol(symbol);
            return s;
        });

        // Welford-style incremental mean/stddev update on returns, not raw price —
        // returns are what's comparable across symbols with different price levels
        MarketTick previous = tickRepository.findFirstBySymbolOrderByTimestampDesc(symbol).orElse(null);
        if (previous != null && previous.getPrice() != null && previous.getPrice() > 0) {
            double ret = (newPrice - previous.getPrice()) / previous.getPrice();
            long n = stats.getSampleCount() + 1;
            double oldMean = stats.getMeanReturn();
            double newMean = oldMean + (ret - oldMean) / n;
            double newVarianceAccum = stats.getStddevReturn() * stats.getStddevReturn() * (n - 1)
                    + (ret - oldMean) * (ret - newMean);
            double newStddev = n > 1 ? Math.sqrt(Math.max(newVarianceAccum / n, 1e-8)) : stats.getStddevReturn();

            stats.setMeanReturn(newMean);
            stats.setStddevReturn(newStddev);
            stats.setSampleCount(n);
        }

        statsRepository.save(stats);
    }
}