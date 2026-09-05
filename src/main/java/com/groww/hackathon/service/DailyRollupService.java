package com.groww.hackathon.service;

import com.groww.hackathon.dto.WatchlistItemView;
import com.groww.hackathon.model.ChangeContext;
import com.groww.hackathon.model.ChangeSeverity;
import com.groww.hackathon.model.DailySymbolStat;
import com.groww.hackathon.repository.DailySymbolStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyRollupService {

    private final DailySymbolStatRepository repository;

    public record DailyRollupSummary(List<DailySymbolStat> stats, int totalMarketWideEvents,
                                     int totalIsolatedEvents, String loudestSymbol, String quietestSymbol) {}

    @Transactional
    public void recordScan(String userId, List<WatchlistItemView> items) {
        LocalDate today = LocalDate.now();
        items.forEach(item -> recordItem(userId, today, item));
    }

    private void recordItem(String userId, LocalDate today, WatchlistItemView item) {
        DailySymbolStat stat = repository.findByUserIdAndSymbolAndStatDate(userId, item.getSymbol(), today)
                .orElseGet(() -> {
                    DailySymbolStat fresh = new DailySymbolStat();
                    fresh.setUserId(userId);
                    fresh.setSymbol(item.getSymbol());
                    fresh.setStatDate(today);
                    return fresh;
                });

        String severityName = item.getSeverity() == null ? null : item.getSeverity().name();
        if (severityName != null && !severityName.equals(stat.getLastSeverity())) {
            if (item.getSeverity() == ChangeSeverity.NOTABLE) {
                stat.setNotableEvents(stat.getNotableEvents() + 1);
            } else if (item.getSeverity() == ChangeSeverity.SIGNIFICANT) {
                stat.setSignificantEvents(stat.getSignificantEvents() + 1);
            }
            stat.setLastSeverity(severityName);
        }

        String contextName = item.getContext() == null ? ChangeContext.NONE.name() : item.getContext().name();
        if (!contextName.equals(stat.getLastContext())) {
            if (item.getContext() == ChangeContext.MARKET_WIDE) {
                stat.setMarketWideEvents(stat.getMarketWideEvents() + 1);
            } else if (item.getContext() == ChangeContext.ISOLATED) {
                stat.setIsolatedEvents(stat.getIsolatedEvents() + 1);
            }
            stat.setLastContext(contextName);
        }

        stat.setUpdatedAt(Instant.now());
        repository.save(stat);
    }

    public DailyRollupSummary getRollup(String userId) {
        List<DailySymbolStat> stats = repository.findByUserIdAndStatDate(userId, LocalDate.now());

        int totalMarketWide = stats.stream().mapToInt(DailySymbolStat::getMarketWideEvents).sum();
        int totalIsolated = stats.stream().mapToInt(DailySymbolStat::getIsolatedEvents).sum();

        String loudest = stats.stream()
                .filter(s -> s.getNotableEvents() + s.getSignificantEvents() > 0)
                .max(Comparator.comparingInt(s -> s.getNotableEvents() + s.getSignificantEvents()))
                .map(DailySymbolStat::getSymbol)
                .orElse(null);

        String quietest = stats.stream()
                .filter(s -> s.getNotableEvents() + s.getSignificantEvents() == 0)
                .map(DailySymbolStat::getSymbol)
                .findFirst()
                .orElse(null);

        return new DailyRollupSummary(stats, totalMarketWide, totalIsolated, loudest, quietest);
    }
}