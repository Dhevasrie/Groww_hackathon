package com.groww.hackathon.service;

import com.groww.hackathon.model.ChangeContext;
import com.groww.hackathon.model.ChangeSeverity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.groww.hackathon.service.MarketContextService.ContextualizedChange;
import static com.groww.hackathon.service.MarketContextService.RawChange;
import static org.assertj.core.api.Assertions.assertThat;

class MarketContextServiceTest {

    private final MarketContextService service = new MarketContextService();

    @Test
    void fewMoversAreNeverMarketWide() {
        List<RawChange> changes = List.of(
                new RawChange("RELIANCE", ChangeSeverity.SIGNIFICANT, 3.0),
                new RawChange("TCS", ChangeSeverity.QUIET, 0.1),
                new RawChange("INFY", ChangeSeverity.QUIET, -0.2)
        );

        List<ContextualizedChange> result = service.annotate(changes);

        assertThat(result).filteredOn(c -> c.symbol().equals("RELIANCE"))
                .extracting(ContextualizedChange::context)
                .containsExactly(ChangeContext.ISOLATED);
    }

    @Test
    void correlatedMoveAcrossMostOfTheWatchlistIsTaggedMarketWide() {
        List<RawChange> changes = List.of(
                new RawChange("RELIANCE", ChangeSeverity.SIGNIFICANT, 2.5),
                new RawChange("TCS", ChangeSeverity.NOTABLE, 1.8),
                new RawChange("INFY", ChangeSeverity.NOTABLE, 2.1),
                new RawChange("HDFCBANK", ChangeSeverity.QUIET, 0.3)
        );

        List<ContextualizedChange> result = service.annotate(changes);

        assertThat(result)
                .filteredOn(c -> List.of("RELIANCE", "TCS", "INFY").contains(c.symbol()))
                .extracting(ContextualizedChange::context)
                .containsOnly(ChangeContext.MARKET_WIDE);
    }

    @Test
    void oneOutlierAgainstAMarketWideTrendStaysIsolated() {
        List<RawChange> changes = List.of(
                new RawChange("RELIANCE", ChangeSeverity.NOTABLE, 1.5),
                new RawChange("TCS", ChangeSeverity.NOTABLE, 1.7),
                new RawChange("HDFCBANK", ChangeSeverity.NOTABLE, 1.4),
                // moving the opposite way while everything else rallies — the real story
                new RawChange("ZOMATO", ChangeSeverity.SIGNIFICANT, -4.2)
        );

        List<ContextualizedChange> result = service.annotate(changes);

        assertThat(result).filteredOn(c -> c.symbol().equals("ZOMATO"))
                .extracting(ContextualizedChange::context)
                .containsExactly(ChangeContext.ISOLATED);
        assertThat(result).filteredOn(c -> c.symbol().equals("RELIANCE"))
                .extracting(ContextualizedChange::context)
                .containsExactly(ChangeContext.MARKET_WIDE);
    }
}