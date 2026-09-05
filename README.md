# Smart Market Watchlist

Not a stock tracker — a change-detection system. It doesn't just show you prices; it tells you whether a move is actually unusual for *that specific stock*, whether it's part of a market-wide swing or an isolated outlier, and it gets quieter over time about the things you personally don't care about.

Built for the "Code, by Groww" hackathon.

---

## Why this isn't the obvious watchlist

The brief explicitly asks: what counts as a meaningful change, how does state persist across sessions/devices, how do you handle stale/delayed/conflicting data, how does it scale. Those aren't UI questions — they're the actual engineering problems this project is built around. A naive watchlist answers all four with "it doesn't" (flat % change, browser localStorage, trust whatever the API returns, doesn't need to scale because it's a toy). This one answers each of them deliberately:

| Question from the brief | What we built |
|---|---|
| What counts as "meaningful"? | A z-score against each symbol's own rolling volatility, not a flat % threshold. A 2% move means something different on HDFCBANK than on ZOMATO. |
| How does state persist across sessions/devices? | Server-side `UserViewState` per (user, symbol) — "what you last saw" lives in Postgres, keyed on identity, not in the browser. |
| How do you handle stale/delayed/conflicting data? | Explicit `FRESH` / `STALE` / `CONFLICTED` status surfaced to the user, computed from tick recency and cross-source disagreement — never silently guessed. |
| How does it scale? | Ingestion (price processing, rolling stats) runs once per symbol per tick, independent of watcher count. Personalized diffing only happens when a user actually opens their watchlist, not on every tick for every user. |

---

## What it does

- **Add/remove symbols** to a personal watchlist (search-as-you-type on the add form).
- **Live-updating table** (5s poll) showing current price, a plain-English change message, and a severity badge: `NEW` / `QUIET` / `NOTABLE` / `SIGNIFICANT`.
- **Market-context tagging** — when a move happens, it's tagged `Market-wide` (several holdings moved together) or `Outlier` (this one moved alone), computed fresh across your whole watchlist on each read.
- **One-line digest banner** — a plain-English headline summarizing the most notable thing happening right now, generated from the same data already computed for the table (no separate model, no extra state).
- **Explainability panel** — expand any row to see the actual z-score and your current sensitivity multiplier for that symbol, so "why did this get flagged" is never a black box.
- **Personalized adaptive sensitivity** — dismiss a NOTABLE/SIGNIFICANT alert as "not useful" and that symbol's threshold rises 15% (capped at 3x), so the system quietens down on things you've told it you don't care about. Reversible from a settings page.
- **Change history log** — an append-only record of SIGNIFICANT events per user, deduplicated within a 30-second window so a single volatile burst doesn't spam the log.
- **Daily insights rollup** — a simple per-day counter view of scan/severity activity.
- **System status strip** — shows how many symbols are being polled and how long ago the last successful poll was, so the system's own health is visible, not assumed.
- **Symbol detail page** — full tick history and a chart for any symbol, independent of your watchlist membership.

---

## Architecture

**Stack:** Java 17, Spring Boot 3.3.4, Maven, PostgreSQL, Thymeleaf (server-rendered), vanilla JS for polling/DOM patching (no frontend framework — deliberate, see Trade-offs).

**Layering:** Controller → Service → Repository, consistent across every feature added.

### Data model

| Table | Purpose |
|---|---|
| `watchlist_item` | Which symbols each user is tracking. |
| `market_tick` | Raw ingested price data, append-only, tagged with a source id. |
| `symbol_stats` | One row per symbol: rolling mean/stddev of *returns* (not raw price), updated incrementally on every tick. Shared across all users watching that symbol — this is what keeps ingestion cost independent of watcher count. |
| `user_view_state` | Per (user, symbol): the price/timestamp/tick-id the user last actually saw. This is the entire mechanism behind "return later and see what changed." |
| `user_symbol_sensitivity` | Per (user, symbol): a threshold multiplier (default 1.0, capped 3.0) that rises 15% per dismissal. |
| `change_event_log` | Append-only log of SIGNIFICANT events per user, 30s deduplication window. |
| `daily_symbol_stat` | Daily rollup counters, written on scan. |

### Core flow — opening the watchlist

1. For each symbol on the user's list: fetch latest `market_tick`, fetch `user_view_state` (their last-seen snapshot), fetch `symbol_stats` (that symbol's normal volatility).
2. Compute `percentChange` from last-seen → current, then `zScore = percentChange / stddev`.
3. Classify severity against `zScore`, adjusted by the user's personal `thresholdMultiplier` for that symbol.
4. Assess freshness: is the latest tick recent enough, and do the two most recent ticks (if from different sources) agree closely enough?
5. Across the whole batch, tag each move as market-wide or isolated by comparing it to how many other holdings moved in the same direction at the same time.
6. Log the event if SIGNIFICANT (respecting dedup), roll it into the day's counters.
7. **Update `user_view_state` to the current tick** — opening the watchlist is what "acknowledges" the current state, the same way opening a chat thread marks it read. This is the mechanism that makes "what changed since I last looked" literal rather than approximate.
8. Return the assembled view; the frontend renders it and polls the same endpoint every 5s to keep it live.

### Why ingestion and diffing are separate concerns

A price tick arriving updates `market_tick` and `symbol_stats` exactly once, regardless of how many users are watching that symbol. The personalized, "does this matter to *this* user" computation only runs when that specific user opens their watchlist. This is the direct answer to "how does this scale with more users and larger watchlists" — the expensive, personalized part scales with *active viewing*, not with *total watchers × tick frequency*.

---

## Key decisions and trade-offs 

- **Simulated market feed, not a real API.** The `MarketDataSource` interface has exactly one implementation swapped in (`SimulatedMarketDataSource`) that does a per-symbol random walk with distinct volatility profiles per stock, plus deliberate ~8% dropped-tick and ~5% conflicting-source injection so staleness and conflict handling are actually exercised and demonstrable on demand — not theoretical. Swapping to a real feed (Finnhub, Alpha Vantage) means writing one new class behind the same interface; nothing else in the system changes. Chosen over a real API specifically to avoid rate-limit or downtime risk during a live judged walkthrough.
- **PostgreSQL over H2.** H2 in-memory wipes on restart, which would directly contradict the "state persists across sessions" pitch. Using `schema.sql` + `ddl-auto=validate` (not `update`) so any schema drift fails loudly at startup instead of silently succeeding — a deliberate choice once the schema stabilized, favoring a loud failure over a quiet inconsistency.
- **Thymeleaf, not React.** Server-rendered pages plus plain JS polling gave a real, working end-to-end UI without adding a build pipeline or component-state-sync risk on a hard clock. The trade-off is explicit: less interactive polish than a SPA would allow, in exchange for lower implementation risk.
- **Cookie-based anonymous identity, not real auth.** A `GROWW_UID` cookie stands in for a logged-in user. This is per-browser, not truly cross-device, and that's a known limitation — but everything else in the system (watchlist, view-state, sensitivity, history) is already keyed on a `userId` string, so swapping in real authentication is a substitution at one layer, not an architecture change.
- **Rolling stats on returns, not raw price.** Comparing raw price moves across symbols at different price levels (₹200 vs ₹3800) is meaningless; returns are. The incremental (Welford-style) update keeps this O(1) per tick instead of recomputing over history each time.
- **z-score thresholds, adjustable per user per symbol, not fixed.** A flat "significant" line doesn't account for a symbol's own normal behavior, or for the fact that different users find different symbols noisy. Both problems are solved by the same mechanism: the same z-score classifier, parameterized by a per-(user, symbol) multiplier that adapts from dismissals.

---


## Edge cases handled

- Symbol with no market data yet → shown as `NEW`, no crash, no divide-by-zero.
- User's first-ever view of a symbol (no `UserViewState` row) → classified `NEW` rather than computing a nonsensical diff against nothing.
- Symbol with fewer than 5 ticks of history → falls back to a default volatility assumption instead of trusting an unstable, barely-sampled stddev (which was previously producing absurd z-scores like "4x normal volatility" on a symbol that had only just started).
- Two data sources disagreeing beyond a threshold → flagged `CONFLICTED` rather than silently picking one.
- Feed gone quiet longer than ~4 polling cycles → flagged `STALE` rather than showing a confidently wrong "current" price.
- Repeated SIGNIFICANT events for the same symbol within 30 seconds → deduplicated in the history log rather than spammed.

---

## Setup

**Requirements:** JDK 17 specifically (JDK 23/24 break Lombok's compiler-internals usage — if you hit `java.lang.ExceptionInInitializerError` mentioning `TypeTag`, this is why). PostgreSQL running locally.

1. Install JDK 17 (Adoptium Temurin recommended: https://adoptium.net/temurin/releases/?version=17).
2. Create the database:
   ```sql
   CREATE DATABASE groww_hackathon;
   ```
3. Confirm `src/main/resources/application.properties` points at your local Postgres (default assumes `localhost:5432`, user `postgres`, password `postgres` — adjust if different).
4. In IntelliJ: set JDK 17 as both the Project SDK (File → Project Structure) and Maven's JDK (Settings → Build Tools → Maven → **both** Importing and Runner tabs).
5. Reload Maven, then run `HackathonApplication`.
6. Open `http://localhost:8080/watchlist`. Add a symbol from: `RELIANCE, TCS, INFY, ZOMATO, PAYTM, HDFCBANK, ICICIBANK, TATAMOTORS`.
7. Wait a few poll cycles (5s each) and watch the table update live as the simulated feed moves prices.

No `mvnw` wrapper is included in this project — build via IntelliJ's Maven tool window (Lifecycle → compile/test) if `mvn` isn't on your system PATH.

---

## What I'd do with more time

- Swap the simulated feed for a real one (Finnhub/Alpha Vantage) behind the existing `MarketDataSource` interface — no other change required.
- Move schema management to Flyway/Liquibase instead of `schema.sql` + `ddl-auto=validate`.
- Replace cookie identity with real authentication.
- Store point-in-time `symbol_stats` snapshots so the symbol detail replay reflects volatility *as it was* at each historical moment, not current volatility applied retroactively.
- Derive the tracked-symbol universe dynamically from whatever's currently on any user's watchlist, instead of a fixed hardcoded list.