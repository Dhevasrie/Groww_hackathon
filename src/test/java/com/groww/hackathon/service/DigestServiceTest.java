package com.groww.hackathon.service;

import com.groww.hackathon.dto.WatchlistItemView;
import com.groww.hackathon.model.ChangeContext;
import com.groww.hackathon.model.ChangeSeverity;
import com.groww.hackathon.model.DataFreshness;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DigestServiceTest {

    private final DigestService service = new DigestService();

    private WatchlistItemView view(String symbol, ChangeSeverity severity, Double pct, Double z, ChangeContext context) {
        return new WatchlistItemView(symbol, 100.0, 99.0, pct, z, severity,
                DataFreshness.FRESH, Instant.now(), "msg", context);
    }

    @Test
    void emptyWatchlistProducesNoHeadline() {
        assertThat(service.buildHeadline(List.of())).isEmpty();
    }

    @Test
    void allQuietProducesReassuringHeadline() {
        List<WatchlistItemView> items = List.of(
                view("RELIANCE", ChangeSeverity.QUIET, 0.1, 0.2, ChangeContext.NONE),
                view("TCS", ChangeSeverity.QUIET, -0.2, 0.3, ChangeContext.NONE)
        );

        Optional<String> headline = service.buildHeadline(items);

        assertThat(headline).isPresent();
        assertThat(headline.get()).contains("Nothing significant");
    }

    @Test
    void singleMoverIsDescribedDirectly() {
        List<WatchlistItemView> items = List.of(
                view("RELIANCE", ChangeSeverity.SIGNIFICANT, 4.2, 3.0, ChangeContext.ISOLATED),
                view("TCS", ChangeSeverity.QUIET, 0.1, 0.2, ChangeContext.NONE)
        );

        String headline = service.buildHeadline(items).orElseThrow();

        assertThat(headline).isEqualTo("RELIANCE moved up 4.2%, 3.0x its normal swing.");
    }

    @Test
    void marketWideEventCallsOutTheOutlierSeparately() {
        List<WatchlistItemView> items = List.of(
                view("RELIANCE", ChangeSeverity.NOTABLE, 1.5, 1.3, ChangeContext.MARKET_WIDE),
                view("HDFCBANK", ChangeSeverity.NOTABLE, 1.4, 1.2, ChangeContext.MARKET_WIDE),
                view("INFY", ChangeSeverity.NOTABLE, 1.6, 1.4, ChangeContext.MARKET_WIDE),
                view("TCS", ChangeSeverity.SIGNIFICANT, -4.2, 3.0, ChangeContext.ISOLATED),
                view("ZOMATO", ChangeSeverity.QUIET, 0.1, 0.1, ChangeContext.NONE)
        );

        String headline = service.buildHeadline(items).orElseThrow();

        assertThat(headline).startsWith("3 of 5 holdings moved together");
        assertThat(headline).contains("TCS is the outlier, down 4.2%, 3.0x its normal swing.");
    }

    @Test
    void independentMoversWithNoMarketWideClusterMentionCountAndTopMover() {
        List<WatchlistItemView> items = List.of(
                view("RELIANCE", ChangeSeverity.NOTABLE, 1.2, 1.1, ChangeContext.ISOLATED),
                view("PAYTM", ChangeSeverity.SIGNIFICANT, 5.5, 3.4, ChangeContext.ISOLATED),
                view("TCS", ChangeSeverity.QUIET, 0.1, 0.1, ChangeContext.NONE)
        );

        String headline = service.buildHeadline(items).orElseThrow();

        assertThat(headline).startsWith("2 holdings moved independently");
        assertThat(headline).contains("PAYTM is the outlier, up 5.5%, 3.4x its normal swing.");
    }
}