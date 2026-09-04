// Polls the existing REST API (GET /api/watchlist) every 5s and patches
// the table in place, so severity/freshness/price update live without a
// full page reload. Falls back gracefully if the table doesn't exist yet
// (empty watchlist state).

const POLL_INTERVAL_MS = 5000;

function formatPrice(value) {
    return Number(value).toFixed(2);
}

function updateRow(row, item) {
    const priceCell = row.querySelector('.price-cell');
    const messageCell = row.querySelector('.message-cell');
    const badge = row.querySelector('.badge');
    const freshness = row.querySelector('.freshness');

    const priceChanged = priceCell.textContent.trim() !== formatPrice(item.currentPrice);

    priceCell.textContent = formatPrice(item.currentPrice);
    messageCell.textContent = item.message;

    badge.textContent = item.severity;
    badge.className = 'badge badge-' + item.severity;

    freshness.textContent = item.freshness;
    freshness.className = 'freshness freshness-' + item.freshness;

    row.className = 'row-' + item.severity;

    if (priceChanged) {
        // restart the flash animation
        row.classList.remove('row-flash');
        void row.offsetWidth; // force reflow so the animation restarts
        row.classList.add('row-flash');
    }
}

async function refreshWatchlist() {
    try {
        const res = await fetch('/api/watchlist', { headers: { 'Accept': 'application/json' } });
        if (!res.ok) return;
        const items = await res.json();

        const table = document.getElementById('watchlist-table');
        if (!table) return; // empty state currently shown, nothing to patch

        const tbody = table.querySelector('tbody');
        items.forEach(item => {
            const row = tbody.querySelector(`tr[data-symbol="${item.symbol}"]`);
            if (row) {
                updateRow(row, item);
            }
            // Note: newly-added symbols require a page reload to appear as a
            // new <tr> (the add form already does a redirect for this).
            // Polling only patches rows that already exist in the DOM.
        });
    } catch (err) {
        // Silently ignore transient network errors during polling; don't
        // spam the console or disrupt the page for a single missed poll.
        console.debug('watchlist refresh skipped:', err.message);
    }
}

setInterval(refreshWatchlist, POLL_INTERVAL_MS);
