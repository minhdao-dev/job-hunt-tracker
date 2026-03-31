const TOKEN_KEY = 'jht_access_token';

const Auth = {
    getToken() {
        return sessionStorage.getItem(TOKEN_KEY);
    },

    setToken(token) {
        sessionStorage.setItem(TOKEN_KEY, token);
    },

    removeToken() {
        sessionStorage.removeItem(TOKEN_KEY);
    },

    isLoggedIn() {
        return !!this.getToken();
    },

    requireAuth() {
        if (!this.isLoggedIn()) {
            window.location.href = '/login';
        }
    },

    async apiFetch(url, options = {}) {
        const token = this.getToken();
        const headers = {
            'Content-Type': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
            ...options.headers,
        };

        const response = await fetch(url, { ...options, headers });

        if (response.status === 401) {
            const refreshed = await this.tryRefresh();
            if (!refreshed) {
                this.logout();
                return null;
            }
            return this.apiFetch(url, options);
        }

        return response;
    },

    async tryRefresh() {
        try {
            const res = await fetch('/api/v1/auth/refresh', { method: 'POST' });
            if (!res.ok) return false;
            const data = await res.json();
            this.setToken(data.data.accessToken);
            return true;
        } catch {
            return false;
        }
    },

    logout() {
        this.removeToken();
        fetch('/api/v1/auth/logout', {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${this.getToken()}` }
        }).finally(() => {
            window.location.href = '/login';
        });
    }
};