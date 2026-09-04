package com.groww.hackathon.service.feed;

public interface MarketDataSource {
    /**
     * Produces one raw tick for the given symbol. May return null to represent
     * "no data this cycle" — used to simulate feed dropouts / staleness.
     */
    RawTick fetchTick(String symbol);

    record RawTick(double price, long volume, String sourceId) {}
}