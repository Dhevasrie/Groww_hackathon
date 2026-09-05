package com.groww.hackathon.service;

import com.groww.hackathon.model.ChangeSeverity;
import com.groww.hackathon.model.MarketTick;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SymbolDetailServiceTest {

    @Mock private MarketTickRepository tickRepository;
    @Mock private SymbolStatsRepository statsRepository;
    @InjectMocks private ChangeDetectionService changeDetectionService;

    private MarketTick tick(double price, Instant time) {
        MarketTick t = new MarketTick();
        t.setSymbol("RELIANCE");
        t.setPrice(price);
        t.setVolume(1000L);
        t.setTimestamp(time);
        t.setSourceId("feed-a");
        return t;
    }

    @Test
    void historyIsReturnedNewestFirstAndOldestPointHasNoBaseline() {
        SymbolDetailService service = new SymbolDetailService(tickRepository, statsRepository, changeDetectionService);

        Instant t0 = Instant.parse("2026-09-05T10:00:00Z");
        List<MarketTick> ticksNewestFirst = List.of(
                tick(105.0, t0.plusSeconds(10)),
                tick(102.0, t0.plusSeconds(5)),
                tick(100.0, t0)
        );
        when(tickRepository.findTop200BySymbolOrderByTimestampDesc("RELIANCE")).thenReturn(ticksNewestFirst);
        when(statsRepository.findById("RELIANCE")).thenReturn(Optional.empty());

        List<SymbolDetailService.TickPoint> history = service.getHistory("RELIANCE");

        assertThat(history).hasSize(3);
        assertThat(history.get(0).timestamp()).isEqualTo(t0.plusSeconds(10)); // newest first
        assertThat(history.get(2).severity()).isEqualTo(ChangeSeverity.NEW);  // oldest tick = no baseline yet
    }
}