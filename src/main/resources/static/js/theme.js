const Theme = {
    KEY: 'jht_theme',

    apply(theme) {
        if (theme === 'light') {
            document.body.classList.add('light');
        } else {
            document.body.classList.remove('light');
        }
        this.updateUI(theme);
    },

    toggle() {
        const current = document.body.classList.contains('light') ? 'light' : 'dark';
        const next = current === 'light' ? 'dark' : 'light';
        localStorage.setItem(this.KEY, next);
        this.apply(next);
    },

    updateUI(theme) {
        const icon = document.getElementById('theme-icon');
        const label = document.getElementById('theme-label');

        const sunPath = `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"/>`;
        const moonPath = `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"/>`;

        if (!icon) return;

        if (theme === 'light') {
            icon.innerHTML = moonPath;
            if (label) label.textContent = 'Dark mode';
        } else {
            icon.innerHTML = sunPath;
            if (label) label.textContent = 'Light mode';
        }
    },

    init() {
        const saved = localStorage.getItem(this.KEY) || 'dark';
        this.apply(saved);
    }
};

Theme.init();