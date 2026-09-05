document.addEventListener('click', (e) => {
    const btn = e.target.closest('.expand-toggle');
    if (!btn) return;
    const symbol = btn.getAttribute('data-symbol');
    const panel = document.querySelector(`tr.explain-row[data-symbol-for="${symbol}"]`);
    if (!panel) return;
    const isOpen = panel.style.display !== 'none';
    panel.style.display = isOpen ? 'none' : '';
    btn.textContent = isOpen ? '▸' : '▾';
});