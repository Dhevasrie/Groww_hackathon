package com.groww.hackathon.service;

import com.groww.hackathon.model.*;
import com.groww.hackathon.repository.MarketTickRepository;
import com.groww.hackathon.repository.SymbolStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeDetectionServiceTest {

    @Mock
    private MarketTickRepository tickRepository;

    @Mock
    private SymbolStatsRepository statsRepository;

    @InjectMocks
    private ChangeDetectionService changeDetectionService;

    private SymbolStats statsWithStddev(double stddev) {
        SymbolStats stats = new SymbolStats();
        stats.setSymbol("RELIANCE");
        stats.setStddevReturn(stddev);
        return stats;
    }

    private MarketTick tick(Long id, String symbol, double price, Instant timestamp, String sourceId) {
        return new MarketTick(id, symbol, price, 1000L, timestamp, sourceId);
    }

    // --- classify() ---

    @Test
    void classify_noLastSeenPrice_returnsNew() {
        ChangeDetectionService.ChangeResult result =
                changeDetectionService.classify("RELIANCE", 2500.0, null);

        assertThat(result.severity()).isEqualTo(ChangeSeverity.NEW);
        assertThat(result.percentChange()).isNull();
        assertThat(result.zScore()).isNull();
    }

    @Test
    void classify_smallChangeRelativeToVolatility_returnsQuiet() {
        // stddev 2% -> a 0.5% move is z=0.25, well under NOTABLE_Z(1.0)
        when(statsRepository.findById("RELIANCE")).thenReturn(Optional.of(statsWithStddev(0.02)));

        ChangeDetectionService.ChangeResult result =
                changeDetectionService.classify("RELIANCE", 100.5, 100.0);

        assertThat(result.severity()).isEqualTo(ChangeSeverity.QUIET);
    }

    @Test
    void classify_moderateChange_returnsNotable() {
        // stddev 1% -> a 1.5% move is z=1.5, between NOTABLE_Z(1.0) and SIGNIFICANT_Z(2.0)
        when(statsRepository.findById("RELIANCE")).thenReturn(Optional.of(statsWithStddev(0.01)));

        ChangeDetectionService.ChangeResult result =
                changeDetectionService.classify("RELIANCE", 101.5, 100.0);

        assertThat(result.severity()).isEqualTo(ChangeSeverity.NOTABLE);
    }

    @Test
    void classify_largeChange_returnsSignificant() {
        // stddev 1% -> a 3% move is z=3.0, well above SIGNIFICANT_Z(2.0)
        when(statsRepository.findById("RELIANCE")).thenReturn(Optional.of(statsWithStddev(0.01)));

        ChangeDetectionService.ChangeResult result =
                changeDetectionService.classify("RELIANCE", 103.0, 100.0);

        assertThat(result.severity()).isEqualTo(ChangeSeverity.SIGNIFICANT);
    }

    @Test
    void classify_noStatsFound_fallsBackToDefaultStddev() {
        // no SymbolStats row at all -> service should fall back to 0.01 rather than throwing
        when(statsRepository.findById("WIPRO")).thenReturn(Optional.empty());

        ChangeDetectionService.ChangeResult result =
                changeDetectionService.classify("WIPRO", 101.0, 100.0);

        // 1% move / fallback stddev 0.01 -> z = 1.0 exactly -> NOTABLE boundary
        assertThat(result.zScore()).isEqualTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(result.severity()).isEqualTo(ChangeSeverity.NOTABLE);
    }

    // --- assessFreshness() ---

    @Test
    void assessFreshness_noTickAtAll_returnsStale() {
        DataFreshness freshness = changeDetectionService.assessFreshness(null, "RELIANCE");
        assertThat(freshness).isEqualTo(DataFreshness.STALE);
    }

    @Test
    void assessFreshness_recentTick_returnsFresh() {
        MarketTick latest = tick(1L, "RELIANCE", 2500.0, Instant.now(), "FEED_A");
        // only one prior tick from the same source -> not a conflict
        when(tickRepository.findTop50BySymbolOrderByTimestampDesc("RELIANCE"))
                .thenReturn(List.of(latest));

        DataFreshness freshness = changeDetectionService.assessFreshness(latest, "RELIANCE");
        assertThat(freshness).isEqualTo(DataFreshness.FRESH);
    }

    @Test
    void assessFreshness_tickOlderThanThreshold_returnsStale() {
        MarketTick latest = tick(1L, "RELIANCE", 2500.0, Instant.now().minusSeconds(30), "FEED_A");
        when(tickRepository.findTop50BySymbolOrderByTimestampDesc("RELIANCE"))
                .thenReturn(List.of(latest));

        DataFreshness freshness = changeDetectionService.assessFreshness(latest, "RELIANCE");
        assertThat(freshness).isEqualTo(DataFreshness.STALE);
    }

    @Test
    void assessFreshness_conflictingSourcesBeyondThreshold_returnsConflicted() {
        Instant now = Instant.now();
        MarketTick latest = tick(2L, "RELIANCE", 2510.0, now, "FEED_B");
        MarketTick previous = tick(1L, "RELIANCE", 2500.0, now.minusSeconds(2), "FEED_A");
        // (2510 - 2500) / 2500 = 0.4% > 0.3% threshold, different sources -> conflict
        when(tickRepository.findTop50BySymbolOrderByTimestampDesc("RELIANCE"))
                .thenReturn(List.of(latest, previous));

        DataFreshness freshness = changeDetectionService.assessFreshness(latest, "RELIANCE");
        assertThat(freshness).isEqualTo(DataFreshness.CONFLICTED);
    }
}