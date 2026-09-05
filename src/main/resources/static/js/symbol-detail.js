(function () {
    if (typeof chartPrices === 'undefined' || chartPrices.length === 0) return;

    const canvas = document.getElementById('price-chart');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    const padding = 24;

    const min = Math.min(...chartPrices);
    const max = Math.max(...chartPrices);
    const range = (max - min) || 1;

    function xFor(i) {
        return padding + (i / (chartPrices.length - 1 || 1)) * (width - padding * 2);
    }
    function yFor(price) {
        return height - padding - ((price - min) / range) * (height - padding * 2);
    }

    function drawBase() {
        ctx.clearRect(0, 0, width, height);

        ctx.strokeStyle = '#9b6bff';
        ctx.lineWidth = 2;
        ctx.beginPath();
        chartPrices.forEach((price, i) => {
            const x = xFor(i);
            const y = yFor(price);
            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        });
        ctx.stroke();

        ctx.lineTo(xFor(chartPrices.length - 1), height - padding);
        ctx.lineTo(xFor(0), height - padding);
        ctx.closePath();
        ctx.fillStyle = 'rgba(155,107,255,0.08)';
        ctx.fill();

        ctx.fillStyle = '#8b8fa3';
        ctx.font = '12px -apple-system, sans-serif';
        ctx.textAlign = 'left';
        ctx.fillText(chartPrices[0].toFixed(2), padding, height - 6);
        ctx.textAlign = 'right';
        ctx.fillText(chartPrices[chartPrices.length - 1].toFixed(2), width - padding, height - 6);
    }

    function nearestIndexForX(mouseX) {
        const clamped = Math.max(padding, Math.min(width - padding, mouseX));
        const ratio = (clamped - padding) / ((width - padding * 2) || 1);
        return Math.round(ratio * (chartPrices.length - 1));
    }

    function drawHoverOverlay(index) {
        drawBase();

        const x = xFor(index);
        const y = yFor(chartPrices[index]);

        // vertical guide line under the cursor
        ctx.strokeStyle = 'rgba(230,232,238,0.25)';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(x, padding);
        ctx.lineTo(x, height - padding);
        ctx.stroke();

        // point marker on the line
        ctx.fillStyle = '#9b6bff';
        ctx.beginPath();
        ctx.arc(x, y, 4, 0, Math.PI * 2);
        ctx.fill();

        // tooltip box: time + price
        const label = (typeof chartLabels !== 'undefined' && chartLabels[index]) ? chartLabels[index] : '';
        const priceText = chartPrices[index].toFixed(2);

        ctx.font = '12px -apple-system, sans-serif';
        const boxWidth = Math.max(ctx.measureText(label).width, ctx.measureText(priceText).width) + 16;
        const boxHeight = 40;

        let boxX = x + 10;
        if (boxX + boxWidth > width - 4) boxX = x - boxWidth - 10;
        let boxY = y - boxHeight - 10;
        if (boxY < 4) boxY = y + 10;

        ctx.fillStyle = '#171a23';
        ctx.strokeStyle = '#262a36';
        ctx.lineWidth = 1;
        ctx.beginPath();
        if (ctx.roundRect) {
            ctx.roundRect(boxX, boxY, boxWidth, boxHeight, 6);
        } else {
            ctx.rect(boxX, boxY, boxWidth, boxHeight);
        }
        ctx.fill();
        ctx.stroke();

        ctx.textAlign = 'left';
        ctx.fillStyle = '#8b8fa3';
        ctx.font = '12px -apple-system, sans-serif';
        ctx.fillText(label, boxX + 8, boxY + 16);
        ctx.fillStyle = '#e6e8ee';
        ctx.font = 'bold 12px -apple-system, sans-serif';
        ctx.fillText(priceText, boxX + 8, boxY + 32);
    }

    canvas.addEventListener('mousemove', function (event) {
        const rect = canvas.getBoundingClientRect();
        const scaleX = canvas.width / rect.width; // canvas is styled at 100% width via CSS
        const mouseX = (event.clientX - rect.left) * scaleX;
        drawHoverOverlay(nearestIndexForX(mouseX));
    });

    canvas.addEventListener('mouseleave', drawBase);

    drawBase();
})();