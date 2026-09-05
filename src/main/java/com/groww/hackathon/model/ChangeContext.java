package com.groww.hackathon.model;

public enum ChangeContext {
    MARKET_WIDE, // moved with most of the watchlist at once — likely a market/sector event
    ISOLATED,    // moved on its own, or against/beyond the crowd — this is what deserves a look
    NONE         // not a significant move either way
}