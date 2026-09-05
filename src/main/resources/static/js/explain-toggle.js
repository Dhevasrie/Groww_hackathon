/**
 * Explain Toggle — Expand/collapse the detail panel
 *
 * Toggles the explain row and adds/removes the 'expanded' class
 * which triggers a CSS rotation on the caret.
 */

document.addEventListener('DOMContentLoaded', function() {
    document.addEventListener('click', function(e) {
        const btn = e.target.closest('.expand-toggle');
        if (!btn) return;

        const symbol = btn.getAttribute('data-symbol');
        const panel = document.querySelector('tr.explain-row[data-symbol-for="' + symbol + '"]');
        if (!panel) return;

        const isOpen = panel.style.display !== 'none';

        // Toggle panel
        panel.style.display = isOpen ? 'none' : 'table-row';

        // Toggle class — CSS handles the caret rotation via .expanded .caret
        btn.classList.toggle('expanded');
    });
});