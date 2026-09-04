package com.groww.hackathon.service;

import com.groww.hackathon.dto.WatchlistItemView;
import com.groww.hackathon.model.*;
import com.groww.hackathon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistItemRepository watchlistItemRepository;
    private final UserViewStateRepository userViewStateRepository;
    private final ChangeDetectionService changeDetectionService;

    public WatchlistItem addSymbol(String userId, String symbol) {
        String normalized = symbol.trim().toUpperCase();
        return watchlistItemRepository.findByUserIdAndSymbol(userId, normalized)
                .orElseGet(() -> watchlistItemRepository.save(
                        new WatchlistItem(null, userId, normalized, Instant.now())));
    }

    public void removeSymbol(String userId, String symbol) {
        watchlistItemRepository.deleteByUserIdAndSymbol(userId, symbol.trim().toUpperCase());
    }

    public List<WatchlistItemView> getWatchlistView(String userId) {
        List<WatchlistItem> items = watchlistItemRepository.findByUserId(userId);

        return items.stream().map(item -> {
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
                    : changeDetectionService.classify(symbol, currentPrice, lastSeenPrice);

            if (latest != null) {
                UserViewState state = viewStateOpt.orElseGet(() ->
                        new UserViewState(null, userId, symbol, null, null, null));
                state.setLastSeenPrice(latest.getPrice());
                state.setLastSeenAt(Instant.now());
                state.setLastSeenTickId(latest.getId());
                userViewStateRepository.save(state);
            }

            return new WatchlistItemView(
                    symbol, currentPrice, lastSeenPrice,
                    result.percentChange(), result.zScore(), result.severity(),
                    freshness, latest != null ? latest.getTimestamp() : null, result.message()
            );
        }).toList();
    }
}