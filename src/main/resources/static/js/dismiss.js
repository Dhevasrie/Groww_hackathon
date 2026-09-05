// Handles the "Not useful" feedback button. Event delegation on the table,
// not per-button listeners — watchlist.js patches row content in place via
// textContent/className every poll but never replaces the button elements
// themselves, so a delegated listener on the table survives every refresh.

(function () {
    const table = document.getElementById('watchlist-table');
    if (!table) return;

    table.addEventListener('click', async function (event) {
        const button = event.target.closest('.dismiss-btn');
        if (!button || button.disabled) return;

        const symbol = button.dataset.symbol;
        button.disabled = true;
        button.textContent = 'Noted';

        try {
            await fetch('/api/watchlist/' + encodeURIComponent(symbol) + '/dismiss', { method: 'POST' });
        } catch (err) {
            console.debug('dismiss failed:', err.message);
            button.disabled = false;
            button.textContent = 'Not useful';
        }
    });
})();