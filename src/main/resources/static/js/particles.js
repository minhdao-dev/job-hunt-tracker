(function () {
    const container = document.getElementById('particles');
    if (!container) return;

    const dots = [
        { size: 3, left: 15, delay: 0,  dur: 12 },
        { size: 2, left: 30, delay: 3,  dur: 15 },
        { size: 4, left: 50, delay: 6,  dur: 10 },
        { size: 2, left: 65, delay: 1,  dur: 18 },
        { size: 3, left: 80, delay: 8,  dur: 13 },
        { size: 2, left: 92, delay: 4,  dur: 16 },
        { size: 5, left: 40, delay: 9,  dur: 11 },
    ];

    dots.forEach(d => {
        const el = document.createElement('div');
        el.className = 'dot';
        el.style.cssText = [
            `width:${d.size}px`,
            `height:${d.size}px`,
            `left:${d.left}%`,
            `bottom:-20px`,
            `animation-delay:${d.delay}s`,
            `animation-duration:${d.dur}s`,
        ].join(';');
        container.appendChild(el);
    });
})();