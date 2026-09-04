CREATE TABLE IF NOT EXISTS app_user (
                                        id            BIGSERIAL PRIMARY KEY,
                                        display_name  VARCHAR(64) NOT NULL UNIQUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS watchlist_item (
                                              id              BIGSERIAL PRIMARY KEY,
                                              user_id         BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    symbol          VARCHAR(16) NOT NULL,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_viewed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, symbol)
    );

CREATE TABLE IF NOT EXISTS price_snapshot (
                                              id           BIGSERIAL PRIMARY KEY,
                                              symbol       VARCHAR(16) NOT NULL,
    price        NUMERIC(18,4) NOT NULL,
    volume       BIGINT,
    fetched_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed    BOOLEAN NOT NULL DEFAULT true
    );

CREATE INDEX IF NOT EXISTS idx_snapshot_symbol_time ON price_snapshot (symbol, fetched_at DESC);
CREATE INDEX IF NOT EXISTS idx_watchlist_user ON watchlist_item (user_id);