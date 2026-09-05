package com.groww.hackathon.service;

import com.groww.hackathon.dto.WatchlistItemView;
import com.groww.hackathon.model.*;
import com.groww.hackathon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistItemRepository watchlistItemRepository;
    private final UserViewStateRepository userViewStateRepository;
    private final ChangeDetectionService changeDetectionService;
    private final MarketContextService marketContextService;

    @Transactional
    public WatchlistItem addSymbol(String userId, String symbol) {
        String normalized = symbol.trim().toUpperCase();
        return watchlistItemRepository.findByUserIdAndSymbol(userId, normalized)
                .orElseGet(() -> watchlistItemRepository.save(
                        new WatchlistItem(null, userId, normalized, Instant.now())));
    }

    @Transactional
    public void removeSymbol(String userId, String symbol) {
        watchlistItemRepository.deleteByUserIdAndSymbol(userId, symbol.trim().toUpperCase());
    }

    @Transactional
    public List<WatchlistItemView> getWatchlistView(String userId) {
        List<WatchlistItem> items = watchlistItemRepository.findByUserId(userId);

        // Pass 1: everything that was already here — per-symbol classification
        // and the UserViewState "mark as seen" side effect, unchanged.
        record PendingView(String symbol, Double currentPrice, Double lastSeenPrice,
                           ChangeDetectionService.ChangeResult result,
                           DataFreshness freshness, Instant lastUpdated) {}

        List<PendingView> pending = items.stream().map(item -> {
            String symbol = item.getSymbol();
            MarketTick latest = changeDetectionService.latestTick(symbol).orElse(null);
            Optional<UserViewState> viewStateOpt =
                    userViewStateRepository.findByUserIdAndSymbol(userId, symbol);

            Double currentPrice = (latest != null) ? latest.getPrice() : null;
            Double lastSeenPrice = viewStateOpt.map(UserViewState::getLastSeenPrice).orElse(null);
            DataFreshness freshness = changeDetectionService.assessFreshness(latest, symbol);

            ChangeDetectionService.ChangeResult result = (currentPrice == null)
                    ? new ChangeDetectionService.ChangeResult(
                    ChangeSeverity.NEW, null, null, "No market data yet for " + symbol)
                    : changeDetectionService.classify(symbol, currentPrice, lastSeenPrice,
                    changeDetectionService.resolveThresholdMultiplier(userId, symbol));

            if (latest != null) {
                UserViewState state = viewStateOpt.orElseGet(() ->
                        new UserViewState(null, userId, symbol, null, null, null));
                state.setLastSeenPrice(latest.getPrice());
                state.setLastSeenAt(Instant.now());
                state.setLastSeenTickId(latest.getId());
                userViewStateRepository.save(state);
            }

            return new PendingView(symbol, currentPrice, lastSeenPrice, result, freshness,
                    latest != null ? latest.getTimestamp() : null);
        }).toList();

        // Pass 2: one market-context read across the whole watchlist at once —
        // this is what lets us tell "the market moved" apart from "this stock moved."
        List<MarketContextService.RawChange> rawChanges = pending.stream()
                .map(p -> new MarketContextService.RawChange(
                        p.symbol(), p.result().severity(), p.result().percentChange()))
                .toList();

        Map<String, ChangeContext> contextBySymbol = new HashMap<>();
        marketContextService.annotate(rawChanges)
                .forEach(c -> contextBySymbol.put(c.symbol(), c.context()));

        return pending.stream()
                .map(p -> new WatchlistItemView(
                        p.symbol(), p.currentPrice(), p.lastSeenPrice(),
                        p.result().percentChange(), p.result().zScore(), p.result().severity(),
                        p.freshness(), p.lastUpdated(), p.result().message(),
                        contextBySymbol.getOrDefault(p.symbol(), ChangeContext.NONE)
                ))
                .toList();
    }
}