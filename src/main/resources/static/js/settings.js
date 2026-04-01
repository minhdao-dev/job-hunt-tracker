let currentSettings = null;

document.addEventListener('DOMContentLoaded', () => {
    loadSettings();
});

// ── LOAD ──────────────────────────────
async function loadSettings() {
    const res = await Auth.apiFetch('/api/v1/users/me/settings');
    if (!res) return;
    const data = await res.json();
    if (!data.data) return;

    currentSettings = data.data;
    populateProfile(data.data.profile);
    populateNotification(data.data.notification);
    populatePreferences(data.data.preferences);
}

function populateProfile(p) {
    if (!p) return;
    document.getElementById('p-fullname').value = p.fullName || '';
    document.getElementById('p-email').value = p.email || '';
    document.getElementById('p-avatar').value = p.avatarUrl || '';
    document.getElementById('p-bio').value = p.bio || '';
}

function populateNotification(n) {
    if (!n) return;
    document.getElementById('n-reminder-enabled').checked = n.reminderEnabled !== false;
    document.getElementById('n-reminder-days').value = n.reminderAfterDays || 7;
    document.getElementById('n-email-notif').checked = n.emailNotifications !== false;
    document.getElementById('n-timezone').value = n.timezone || 'Asia/Ho_Chi_Minh';
    document.getElementById('n-language').value = n.language || 'vi';
}

function populatePreferences(pref) {
    if (!pref) return;
    document.getElementById('pref-role').value = pref.targetRole || '';
    document.getElementById('pref-sal-min').value = pref.targetSalaryMin || '';
    document.getElementById('pref-sal-max').value = pref.targetSalaryMax || '';
    document.getElementById('pref-location').value = pref.preferredLocation || '';
    document.getElementById('pref-worktype').value = pref.workType || '';
}

// ── SAVE HANDLERS ─────────────────────
async function saveProfile() {
    const btn = document.getElementById('btn-save-profile');
    btn.disabled = true;
    btn.textContent = 'Saving...';

    const res = await Auth.apiFetch('/api/v1/users/me/profile', {
        method: 'PATCH',
        body: JSON.stringify({
            fullName: document.getElementById('p-fullname').value.trim(),
            avatarUrl: document.getElementById('p-avatar').value.trim() || null,
            bio: document.getElementById('p-bio').value.trim() || null,
        })
    });

    btn.disabled = false;
    btn.textContent = 'Save profile';
    res?.ok ? showToast('Profile updated', 'success') : showToast('Failed to save profile', 'error');
}

async function saveNotification() {
    const btn = document.getElementById('btn-save-notification');
    btn.disabled = true;
    btn.textContent = 'Saving...';

    const days = parseInt(document.getElementById('n-reminder-days').value);

    const res = await Auth.apiFetch('/api/v1/users/me/notification', {
        method: 'PATCH',
        body: JSON.stringify({
            reminderEnabled: document.getElementById('n-reminder-enabled').checked,
            reminderAfterDays: isNaN(days) ? 7 : Math.min(30, Math.max(1, days)),
            emailNotifications: document.getElementById('n-email-notif').checked,
            timezone: document.getElementById('n-timezone').value || null,
            language: document.getElementById('n-language').value || null,
        })
    });

    btn.disabled = false;
    btn.textContent = 'Save';
    res?.ok ? showToast('Notification settings saved', 'success') : showToast('Failed to save', 'error');
}

async function savePreferences() {
    const btn = document.getElementById('btn-save-preferences');
    btn.disabled = true;
    btn.textContent = 'Saving...';

    const res = await Auth.apiFetch('/api/v1/users/me/preferences', {
        method: 'PATCH',
        body: JSON.stringify({
            targetRole: document.getElementById('pref-role').value.trim() || null,
            targetSalaryMin: parseInt(document.getElementById('pref-sal-min').value) || null,
            targetSalaryMax: parseInt(document.getElementById('pref-sal-max').value) || null,
            preferredLocation: document.getElementById('pref-location').value.trim() || null,
            workType: document.getElementById('pref-worktype').value || null,
        })
    });

    btn.disabled = false;
    btn.textContent = 'Save';
    res?.ok ? showToast('Preferences saved', 'success') : showToast('Failed to save', 'error');
}

// ── CHANGE EMAIL ──────────────────────
async function changeEmail() {
    const newEmail = document.getElementById('ce-email').value.trim();
    const password = document.getElementById('ce-password').value;

    if (!newEmail || !password) {
        showError('security-error', 'Please fill in all fields');
        return;
    }

    const btn = document.getElementById('btn-change-email');
    btn.disabled = true;
    btn.textContent = 'Sending...';

    const res = await Auth.apiFetch('/api/v1/users/me/change-email', {
        method: 'POST',
        body: JSON.stringify({newEmail, currentPassword: password})
    });

    btn.disabled = false;
    btn.textContent = 'Change email';

    if (res?.ok) {
        document.getElementById('ce-email').value = '';
        document.getElementById('ce-password').value = '';
        showToast('Verification email sent to new address', 'success');
    } else {
        const err = await res?.json();
        showError('security-error', err?.detail || 'Failed to change email');
    }
}

// ── CHANGE PASSWORD ───────────────────
async function changePassword() {
    const current = document.getElementById('cp-current').value;
    const newPw = document.getElementById('cp-new').value;

    if (!current || !newPw) {
        showError('security-error', 'Please fill in all fields');
        return;
    }
    if (newPw.length < 8) {
        showError('security-error', 'New password must be at least 8 characters');
        return;
    }

    const btn = document.getElementById('btn-change-password');
    btn.disabled = true;
    btn.textContent = 'Saving...';

    const res = await Auth.apiFetch('/api/v1/auth/change-password', {
        method: 'PUT',
        body: JSON.stringify({currentPassword: current, newPassword: newPw})
    });

    btn.disabled = false;
    btn.textContent = 'Change password';

    if (res?.ok) {
        document.getElementById('cp-current').value = '';
        document.getElementById('cp-new').value = '';
        showToast('Password changed. Please login again.', 'success');
        setTimeout(() => Auth.logout(), 2000);
    } else {
        const err = await res?.json();
        showError('security-error', err?.detail || 'Failed to change password');
    }
}

// ── DELETE ACCOUNT ────────────────────
async function deleteAccount() {
    const password = document.getElementById('da-password').value;
    if (!password) {
        showError('danger-error', 'Please enter your password');
        return;
    }

    if (!confirm('Are you sure you want to delete your account?\nAll your data will be permanently removed.')) return;

    const res = await Auth.apiFetch('/api/v1/users/me', {
        method: 'DELETE',
        body: JSON.stringify({currentPassword: password})
    });

    if (res?.ok) {
        Auth.removeToken();
        window.location.href = '/login';
    } else {
        const err = await res?.json();
        showError('danger-error', err?.detail || 'Failed to delete account');
    }
}

// ── TABS ──────────────────────────────
function switchSection(name) {
    document.querySelectorAll('.settings-nav-item').forEach(i => i.classList.remove('active'));
    document.querySelectorAll('.settings-panel').forEach(p => p.classList.remove('active'));
    document.getElementById('nav-' + name).classList.add('active');
    document.getElementById('panel-' + name).classList.add('active');
}

// ── HELPERS ───────────────────────────
function showToast(msg, type = 'success') {
    const toast = document.getElementById('toast');
    toast.querySelector('.toast-msg').textContent = msg;
    toast.className = `toast ${type}`;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 3000);
}

function showError(id, msg) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = msg;
    el.style.display = 'block';
}