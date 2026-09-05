package com.groww.hackathon.service.feed;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimulatedMarketDataSource implements MarketDataSource {

    private final Random random = new Random();

    // last known price per symbol, so the random walk is continuous, not re-randomized each tick
    private final Map<String, Double> lastPrice = new ConcurrentHashMap<>();

    // per-symbol "personality" — some stocks are calm, some are wild. This is what makes
    // the z-score classifier meaningful: identical % moves mean different things per symbol.
    private final Map<String, Double> volatilityProfile = Map.of(
            "RELIANCE", 0.006,
            "TCS", 0.005,
            "INFY", 0.008,
            "ZOMATO", 0.025,
            "PAYTM", 0.030,
            "HDFCBANK", 0.004,
            "ICICIBANK", 0.005,
            "TATAMOTORS", 0.018
    );

    private final Map<String, Double> basePrice = Map.of(
            "RELIANCE", 2900.0,
            "TCS", 3800.0,
            "INFY", 1550.0,
            "ZOMATO", 210.0,
            "PAYTM", 650.0,
            "HDFCBANK", 1650.0,
            "ICICIBANK", 1200.0,
            "TATAMOTORS", 950.0
    );

    @Override
    public RawTick fetchTick(String symbol) {
        // simulate a dropped feed cycle ~8% of the time, to exercise staleness handling downstream
        if (random.nextDouble() < 0.08) {
            return null;
        }

        double sigma = volatilityProfile.getOrDefault(symbol, 0.01);
        double prev = lastPrice.getOrDefault(symbol, basePrice.getOrDefault(symbol, 1000.0));

        double drift = random.nextGaussian() * sigma;
        double newPrice = Math.max(1.0, prev * (1 + drift));
        lastPrice.put(symbol, newPrice);

        long volume = 10_000 + (long) (random.nextDouble() * 50_000);

        // simulate two disagreeing sources ~5% of the time, to exercise conflict handling
        String sourceId = random.nextDouble() < 0.05 ? "FEED_B" : "FEED_A";
        if (sourceId.equals("FEED_B")) {
            newPrice = newPrice * (1 + random.nextGaussian() * 0.002); // slight disagreement from "source B"
        }

        return new RawTick(newPrice, volume, sourceId);
    }

    /**
     * The known symbol universe for this demo feed — same source of truth
     * used to seed the random walk. Powers the search-as-you-type
     * autocomplete so the suggestion list can never drift out of sync with
     * what the feed actually simulates prices for.
     */
    public Set<String> knownSymbols() {
        return basePrice.keySet();
    }
}