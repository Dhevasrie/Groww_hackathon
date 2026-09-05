package com.groww.hackathon.service;

import com.groww.hackathon.dto.WatchlistItemView;
import com.groww.hackathon.model.ChangeContext;
import com.groww.hackathon.model.ChangeSeverity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Composes a single plain-language sentence summarizing the whole watchlist,
 * using data ChangeDetectionService and MarketContextService already produced
 * this scan. No new computation, no new state — just turns N rows into one
 * headline a user can read in a second, e.g.:
 *
 *   "3 of 6 holdings moved together — looks market-wide. TCS is the
 *    outlier, up 4.2%, 3.0x its normal swing."
 */
@Service
public class DigestService {

    public Optional<String> buildHeadline(List<WatchlistItemView> items) {
        if (items.isEmpty()) {
            return Optional.empty();
        }

        List<WatchlistItemView> movers = items.stream().filter(this::isMover).toList();

        if (movers.isEmpty()) {
            return Optional.of(String.format(
                    "Nothing significant since you last checked — all %d holding%s within normal range.",
                    items.size(), items.size() == 1 ? " is" : "s are"));
        }

        if (movers.size() == 1) {
            return Optional.of(describeMove(movers.get(0), true));
        }

        long marketWideCount = movers.stream()
                .filter(i -> i.getContext() == ChangeContext.MARKET_WIDE)
                .count();

        List<WatchlistItemView> outliers = movers.stream()
                .filter(i -> i.getContext() == ChangeContext.ISOLATED)
                .sorted(Comparator.comparingDouble((WatchlistItemView i) -> Math.abs(zScoreOrZero(i))).reversed())
                .toList();

        String headline = marketWideCount > 0
                ? String.format("%d of %d holdings moved together — looks market-wide.", marketWideCount, items.size())
                : String.format("%d holdings moved independently since you last checked.", movers.size());

        if (!outliers.isEmpty()) {
            headline += " " + describeMove(outliers.get(0), false);
        }

        return Optional.of(headline);
    }

    private String describeMove(WatchlistItemView item, boolean isSoleMover) {
        double pct = item.getPercentChange() == null ? 0.0 : item.getPercentChange();
        double z = zScoreOrZero(item);
        String direction = pct >= 0 ? "up" : "down";
        String lead = isSoleMover ? item.getSymbol() + " moved" : item.getSymbol() + " is the outlier,";
        return String.format("%s %s %.1f%%, %.1fx its normal swing.", lead, direction, Math.abs(pct), z);
    }

    private double zScoreOrZero(WatchlistItemView item) {
        return item.getZScore() == null ? 0.0 : item.getZScore();
    }

    private boolean isMover(WatchlistItemView item) {
        return item.getSeverity() == ChangeSeverity.NOTABLE || item.getSeverity() == ChangeSeverity.SIGNIFICANT;
    }
}