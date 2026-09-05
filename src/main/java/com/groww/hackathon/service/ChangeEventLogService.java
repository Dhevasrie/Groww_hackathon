package com.groww.hackathon.service;

import com.groww.hackathon.model.ChangeEventLog;
import com.groww.hackathon.model.ChangeSeverity;
import com.groww.hackathon.repository.ChangeEventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChangeEventLogService {

    // ~6 ticks at the poller's interval: long enough that a move which stays
    // SIGNIFICANT across several consecutive polls logs once, not once per poll;
    // short enough that a move which fades and later recurs logs as two events.
    private static final Duration DEDUP_WINDOW = Duration.ofSeconds(30);

    private final ChangeEventLogRepository repository;

    @Transactional
    public void recordIfSignificant(String userId, String symbol,
                                    ChangeDetectionService.ChangeResult result) {
        if (result.severity() != ChangeSeverity.SIGNIFICANT) return;

        boolean recentlyLogged = repository
                .findTopByUserIdAndSymbolOrderByOccurredAtDesc(userId, symbol)
                .map(last -> Duration.between(last.getOccurredAt(), Instant.now())
                        .compareTo(DEDUP_WINDOW) < 0)
                .orElse(false);
        if (recentlyLogged) return;

        ChangeEventLog log = new ChangeEventLog();
        log.setUserId(userId);
        log.setSymbol(symbol);
        log.setPercentChange(result.percentChange());
        log.setZScore(result.zScore());
        log.setMessage(result.message());
        log.setOccurredAt(Instant.now());
        repository.save(log);
    }

    public List<ChangeEventLog> getHistory(String userId) {
        return repository.findByUserIdOrderByOccurredAtDesc(userId);
    }
}