const POLL_INTERVAL_MS = 5000;

function formatPrice(value) {
    return Number(value).toFixed(2);
}

function formatAge(lastUpdated) {
    if (!lastUpdated) return '';
    const seconds = Math.max(0, Math.round((Date.now() - new Date(lastUpdated).getTime()) / 1000));
    return `(${seconds}s old)`;
}

function updateRow(row, item) {
    const priceCell = row.querySelector('.price-cell');
    const messageCell = row.querySelector('.message-cell');
    const badge = row.querySelector('.badge:not(.badge-context-muted):not(.badge-context-outlier)');
    const freshness = row.querySelector('.freshness');
    const ageEl = row.querySelector('.freshness-age');
    const statusBadges = row.querySelector('.status-badges');
    const dismissBtn = row.querySelector('.dismiss-btn');

    const priceChanged = priceCell.textContent.trim() !== formatPrice(item.currentPrice);

    priceCell.textContent = formatPrice(item.currentPrice);
    messageCell.textContent = item.message;

    badge.textContent = item.severity;
    badge.className = 'badge badge-' + item.severity;

    freshness.textContent = item.freshness;
    freshness.className = 'freshness freshness-' + item.freshness;
    if (ageEl) ageEl.textContent = ' ' + formatAge(item.lastUpdated);

    const existingContextBadge = statusBadges.querySelector('.badge-context-muted, .badge-context-outlier');
    if (existingContextBadge) existingContextBadge.remove();
    if (item.context === 'MARKET_WIDE') {
        const span = document.createElement('span');
        span.className = 'badge badge-context-muted';
        span.textContent = 'Market-wide';
        statusBadges.appendChild(span);
    } else if (item.context === 'ISOLATED') {
        const span = document.createElement('span');
        span.className = 'badge badge-context-outlier';
        span.textContent = 'Outlier';
        statusBadges.appendChild(span);
    }

    if (dismissBtn) {
        const shouldShow = item.severity === 'NOTABLE' || item.severity === 'SIGNIFICANT';
        dismissBtn.style.display = shouldShow ? '' : 'none';
    }

    // Keep the explain panel in sync with the same data driving the badge
    // above it — this was the bug: the panel used to freeze at page-load
    // while the badge kept updating live, so they could visibly disagree.
    const explainRow = row.parentElement.querySelector(
        `tr.explain-row[data-symbol-for="${item.symbol}"]`
    );
    if (explainRow) {
        const zEl = explainRow.querySelector('.explain-zscore');
        const sensEl = explainRow.querySelector('.explain-sensitivity');
        if (zEl) zEl.textContent = item.zScore != null ? Number(item.zScore).toFixed(2) : '—';
        if (sensEl) sensEl.textContent = Number(item.thresholdMultiplier).toFixed(2) + 'x';
    }

    row.className = 'row-' + item.severity;

    if (priceChanged) {
        row.classList.remove('row-flash');
        void row.offsetWidth;
        row.classList.add('row-flash');
    }
}

async function refreshWatchlist() {
    try {
        const res = await fetch('/api/watchlist', { headers: { 'Accept': 'application/json' } });
        if (!res.ok) return;
        const items = await res.json();

        const table = document.getElementById('watchlist-table');
        if (!table) return;

        const tbody = table.querySelector('tbody');
        items.forEach(item => {
            const row = tbody.querySelector(`tr[data-symbol="${item.symbol}"]`);
            if (row) {
                updateRow(row, item);
            }
        });
    } catch (err) {
        console.debug('watchlist refresh skipped:', err.message);
    }
}

async function refreshStatus() {
    try {
        const res = await fetch('/api/watchlist/status', { headers: { 'Accept': 'application/json' } });
        if (!res.ok) return;
        const status = await res.json();
        const countEl = document.getElementById('status-symbol-count');
        const pollEl = document.getElementById('status-last-poll');
        if (countEl) countEl.textContent = status.trackedSymbolCount;
        if (pollEl) pollEl.textContent = status.lastPollSecondsAgo;
    } catch (err) {
        console.debug('status refresh skipped:', err.message);
    }
}

async function refreshDigest() {
    try {
        const res = await fetch('/api/watchlist/digest', { headers: { 'Accept': 'application/json' } });
        if (!res.ok) return;
        const data = await res.json();
        const banner = document.getElementById('digest-banner');
        if (!banner) return;
        if (data.headline) {
            banner.textContent = data.headline;
            banner.style.display = '';
        } else {
            banner.style.display = 'none';
        }
    } catch (err) {
        console.debug('digest refresh skipped:', err.message);
    }
}

setInterval(refreshWatchlist, POLL_INTERVAL_MS);
setInterval(refreshStatus, POLL_INTERVAL_MS);
setInterval(refreshDigest, POLL_INTERVAL_MS);
refreshWatchlist();
refreshStatus();
refreshDigest();