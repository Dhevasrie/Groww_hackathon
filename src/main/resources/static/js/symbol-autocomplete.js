// Search-as-you-type on the add-symbol input. Deliberately kept in its own
// file, separate from watchlist.js's polling logic — this only ever touches
// the add-form, never the watchlist table, so the two scripts can't collide.

(function () {
    const input = document.getElementById('symbol-input');
    if (!input) return;

    const form = input.closest('form');
    const dropdown = document.createElement('div');
    dropdown.className = 'autocomplete-dropdown';
    dropdown.style.display = 'none';
    form.appendChild(dropdown);

    const DEBOUNCE_MS = 150;
    let debounceTimer = null;
    let currentMatches = [];
    let activeIndex = -1;

    function renderMatches(matches) {
        currentMatches = matches;
        activeIndex = -1;
        dropdown.innerHTML = '';

        if (matches.length === 0) {
            dropdown.style.display = 'none';
            return;
        }

        matches.forEach(symbol => {
            const item = document.createElement('div');
            item.className = 'autocomplete-item';
            item.textContent = symbol;
            // mousedown, not click — fires before the input's blur event,
            // so the click actually registers instead of the dropdown
            // disappearing first
            item.addEventListener('mousedown', function (e) {
                e.preventDefault();
                selectSymbol(symbol);
            });
            dropdown.appendChild(item);
        });

        dropdown.style.display = 'block';
    }

    function selectSymbol(symbol) {
        input.value = symbol;
        dropdown.style.display = 'none';
        input.focus();
    }

    function highlight(index) {
        Array.from(dropdown.children).forEach((el, i) => {
            el.classList.toggle('active', i === index);
        });
    }

    async function fetchMatches(query) {
        try {
            const res = await fetch('/api/symbols?query=' + encodeURIComponent(query));
            if (!res.ok) return;
            renderMatches(await res.json());
        } catch (err) {
            console.debug('symbol search skipped:', err.message);
        }
    }

    input.addEventListener('input', function () {
        clearTimeout(debounceTimer);
        const query = input.value.trim();
        debounceTimer = setTimeout(() => fetchMatches(query), DEBOUNCE_MS);
    });

    input.addEventListener('keydown', function (e) {
        if (dropdown.style.display === 'none' || currentMatches.length === 0) return;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            activeIndex = Math.min(activeIndex + 1, currentMatches.length - 1);
            highlight(activeIndex);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            activeIndex = Math.max(activeIndex - 1, 0);
            highlight(activeIndex);
        } else if (e.key === 'Enter' && activeIndex >= 0) {
            e.preventDefault();
            selectSymbol(currentMatches[activeIndex]);
        } else if (e.key === 'Escape') {
            dropdown.style.display = 'none';
        }
    });

    document.addEventListener('click', function (e) {
        if (!form.contains(e.target)) {
            dropdown.style.display = 'none';
        }
    });
})();