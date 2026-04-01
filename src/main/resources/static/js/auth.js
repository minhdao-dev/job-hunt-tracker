const TOKEN_KEY = 'jht_access_token';

const Auth = {
    getToken() {
        return localStorage.getItem(TOKEN_KEY);
    },

    setToken(token) {
        localStorage.setItem(TOKEN_KEY, token);
    },

    removeToken() {
        localStorage.removeItem(TOKEN_KEY);
    },

    isLoggedIn() {
        return !!this.getToken();
    },

    requireAuth() {
        if (!this.isLoggedIn()) {
            window.location.href = '/login';
        }
    },

    // Wrapper fetch tự động attach Bearer token
    // Nếu 401 → thử refresh 1 lần → retry request gốc
    // Nếu refresh fail → logout
    async apiFetch(url, options = {}) {
        const token = this.getToken();
        const headers = {
            'Content-Type': 'application/json',
            ...(token ? {'Authorization': `Bearer ${token}`} : {}),
            ...options.headers,
        };

        let response = await fetch(url, {...options, headers});

        if (response.status === 401) {
            const refreshed = await this.tryRefresh();
            if (refreshed) {
                // Retry với token mới
                const newToken = this.getToken();
                const retryHeaders = {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${newToken}`,
                    ...options.headers,
                };
                response = await fetch(url, {...options, headers: retryHeaders});
            } else {
                this.logout();
                return null;
            }
        }

        return response;
    },

    // Gọi refresh endpoint, lưu token mới nếu thành công
    async tryRefresh() {
        try {
            const res = await fetch('/api/v1/auth/refresh', {
                method: 'POST',
                credentials: 'same-origin'
            });
            if (!res.ok) return false;
            const data = await res.json();
            if (data?.data?.accessToken) {
                this.setToken(data.data.accessToken);
                return true;
            }
            return false;
        } catch {
            return false;
        }
    },

    // Logout: gọi API revoke token, xóa localStorage, redirect login
    logout() {
        const token = this.getToken();
        this.removeToken();

        if (token) {
            fetch('/api/v1/auth/logout', {
                method: 'POST',
                credentials: 'same-origin',
                headers: {'Authorization': `Bearer ${token}`}
            }).catch(() => {
            }).finally(() => {
                window.location.href = '/login';
            });
        } else {
            window.location.href = '/login';
        }
    }
};